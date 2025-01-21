package com.julienphalip.ideavim.peekaboo;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.Gray;
import com.intellij.ui.awt.RelativePoint;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.OperatorArguments;
import com.maddyhome.idea.vim.extension.ExtensionHandler;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.maddyhome.idea.vim.helper.EngineStringHelper;
import com.maddyhome.idea.vim.key.KeyMapping;
import com.maddyhome.idea.vim.key.MappingInfo;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import com.maddyhome.idea.vim.register.Register;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

public class Peekaboo implements VimExtension {
    // UI Colors
    private static final class Colors {
        static final String REGISTER = "#FFFFFF";
        static final String BACKGROUND = "#2b2b2b";
        static final String TEXT = "#A9B7C6";
        static final String HEADER = "#FFC66D";
        static final String COMMENT = "#808080";
    }

    // Register descriptions and configurations
    private static final class RegisterConfig {
        static final Map<String, String> DESCRIPTIONS =
                new HashMap<>() {
                    {
                        put("\"", "unnamed register: last deleted, changed, or yanked content");
                        put("-", "deleted or changed content smaller than one line");
                        put(":", "last executed command");
                        put(".", "last inserted text");
                        put("%", "name of the current file");
                        put("#", "name of the alternate file");
                        put("=", "expression register");
                        put("/", "last search pattern");
                        put("*", "system clipboard");
                        put("+", "system selection primary");
                    }
                };

        static final String[] SPECIAL_REGISTERS = {
            "\"", "*", "+", "%", "#", ".", ":", "/", "=", "-"
        };
    }

    @Override
    public @NotNull String getName() {
        return "peekaboo";
    }

    @Override
    public void init() {
        // Register quote (") for normal mode and Ctrl+R for insert mode
        registerKeyMapping(
                MappingMode.NORMAL, Collections.singletonList(KeyStroke.getKeyStroke('"')));
        registerKeyMapping(
                MappingMode.INSERT, Collections.singletonList(KeyStroke.getKeyStroke("control R")));
    }

    // Helper method to register key mappings for a specific mode
    private void registerKeyMapping(MappingMode mode, List<KeyStroke> keyStrokes) {
        KeyMapping modeKeyMapping = VimPlugin.getKey().getKeyMapping(mode);

        // Handle existing mappings
        List<Pair<List<KeyStroke>, MappingInfo>> mappings = modeKeyMapping.getMapTo(keyStrokes);
        for (Pair<List<KeyStroke>, MappingInfo> mapping : mappings) {
            putKeyMapping(mode, mapping.getSecond().getFromKeys(), keyStrokes);
        }

        // Add default mapping if none exists
        if (modeKeyMapping.get(keyStrokes) == null) {
            putKeyMapping(mode, keyStrokes, keyStrokes);
        }
    }

    private void putKeyMapping(
            MappingMode mode, List<KeyStroke> fromKeys, List<KeyStroke> originalKeys) {
        VimPlugin.getKey()
                .putKeyMapping(
                        EnumSet.of(mode),
                        fromKeys,
                        getOwner(),
                        new ShowRegistersHandler(originalKeys),
                        false);
    }

    private record ShowRegistersHandler(List<KeyStroke> originalKeyStrokes)
            implements ExtensionHandler {
        @Override
        public void execute(
                @NotNull VimEditor vimEditor,
                @NotNull ExecutionContext context,
                @NotNull OperatorArguments operatorArguments) {
            // Re-execute the original key sequence
            KeyHandler keyHandler = KeyHandler.getInstance();
            for (KeyStroke keyStroke : originalKeyStrokes) {
                keyHandler.handleKey(
                        vimEditor,
                        keyStroke,
                        context,
                        false,
                        false,
                        keyHandler.getKeyHandlerState());
            }
            showPopup(vimEditor);
        }

        private void showPopup(VimEditor vimEditor) {
            StringBuilder html = new StringBuilder(createHtmlHeader());

            // Build register sections
            appendRegisterSection(html, "Special Registers", RegisterConfig.SPECIAL_REGISTERS);
            appendNamedRegistersSection(html);
            appendLastYankSection(html);
            appendNumberedRegistersSection(html);

            html.append("</div></body></html>");
            showBalloon(vimEditor, html.toString());
        }

        private String createHtmlHeader() {
            return String.format(
                    """
                    <html>
                    <body style='margin: 3px; width: 100%%; background-color: %s; color: %s;'>
                    <div style='font-family: monospace; min-width: 600px;'>
                    """,
                    Colors.BACKGROUND, Colors.TEXT);
        }

        private void appendRegisterSection(StringBuilder html, String title, String[] registers) {
            html.append("<div style='margin-bottom: 15px;'>");
            html.append(
                    String.format(
                            "<div style='margin-bottom: 8px; color: %s;'>%s</div>",
                            Colors.HEADER, title));

            for (String reg : registers) {
                Optional.ofNullable(VimPlugin.getRegister().getRegister(reg.charAt(0)))
                        .ifPresent(
                                r -> {
                                    String content = getRegisterText(r);
                                    if (!content.isEmpty()) {
                                        appendRegister(
                                                html,
                                                reg,
                                                content,
                                                RegisterConfig.DESCRIPTIONS.get(reg));
                                    }
                                });
            }
            html.append("</div>");
        }

        private void appendNamedRegistersSection(StringBuilder html) {
            html.append("<div style='margin-bottom: 15px;'>");
            html.append(
                    String.format(
                            "<div style='margin-bottom: 8px; color: %s;'>Named Registers</div>",
                            Colors.HEADER));

            for (char c = 'a'; c <= 'z'; c++) {
                char finalC = c;
                Optional.ofNullable(VimPlugin.getRegister().getRegister(c))
                        .ifPresent(
                                r -> {
                                    String content = getRegisterText(r);
                                    if (!content.isEmpty()) {
                                        appendRegister(html, String.valueOf(finalC), content, null);
                                    }
                                });
            }
            html.append("</div>");
        }

        private void appendLastYankSection(StringBuilder html) {
            html.append("<div style='margin-bottom: 15px;'>");
            html.append(
                    String.format(
                            "<div style='margin-bottom: 8px; color: %s;'>Last Yank</div>",
                            Colors.HEADER));

            Optional.ofNullable(VimPlugin.getRegister().getRegister('0'))
                    .ifPresent(
                            r -> {
                                String content = getRegisterText(r);
                                if (!content.isEmpty()) {
                                    appendRegister(html, "0", content, null);
                                }
                            });
            html.append("</div>");
        }

        private void appendNumberedRegistersSection(StringBuilder html) {
            html.append("<div style='margin-bottom: 15px;'>");
            html.append(
                    String.format(
                            "<div style='margin-bottom: 8px; color: %s;'>Delete/Change History<span"
                                + " style='margin-left: 8px; color: %s;'>(deleted/changed content"
                                + " larger than one line)</span></div>",
                            Colors.HEADER, Colors.COMMENT));

            for (int i = 1; i <= 9; i++) {
                int finalI = i;
                Optional.ofNullable(
                                VimPlugin.getRegister().getRegister(String.valueOf(i).charAt(0)))
                        .ifPresent(
                                reg -> {
                                    String content = getRegisterText(reg);
                                    if (!content.isEmpty()) {
                                        appendRegister(
                                                html,
                                                String.valueOf(finalI),
                                                content,
                                                finalI == 1 ? "most recent" : null);
                                    }
                                });
            }
            html.append("</div>");
        }

        // Helper method to show the balloon popup
        private void showBalloon(VimEditor vimEditor, String htmlContent) {
            Balloon balloon =
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder(htmlContent, null, Gray._43, null)
                            .setAnimationCycle(10)
                            .setHideOnClickOutside(true)
                            .setHideOnKeyOutside(true)
                            .createBalloon();

            Editor ijEditor = IjVimEditorKt.getIj(vimEditor);
            IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(ijEditor.getProject());
            if (ideFrame != null) {
                Point location = new Point(0, ideFrame.getComponent().getHeight() - 100);
                RelativePoint point = new RelativePoint(ideFrame.getComponent(), location);
                balloon.show(point, Balloon.Position.above);
            }
        }

        private String getRegisterText(Register register) {
            List<KeyStroke> text =
                    register.getText() != null
                            ? VimInjectorKt.getInjector().getParser().parseKeys(register.getText())
                            : register.getKeys();
            return EngineStringHelper.INSTANCE
                    .toPrintableCharacters(text)
                    .substring(0, Math.min(text.size(), 200));
        }

        private void appendRegister(
                StringBuilder html, String register, String content, String description) {
            html.append(
                    String.format(
                            """
                            <div style='display: flex; margin-bottom: 4px; align-items: baseline;'>
                                <span style='color: %s; font-weight: bold;'>%s</span>
                                <span style='color: %s;'> → </span>
                                <span style='color: %s;'>%s</span>
                                %s
                            </div>
                            """,
                            Colors.REGISTER,
                            register,
                            Colors.COMMENT,
                            Colors.TEXT,
                            formatRegisterContent(content),
                            description != null
                                    ? String.format(
                                            "<span style='color: %s; margin-left:"
                                                    + " 8px;'>(%s)</span>",
                                            Colors.COMMENT, description)
                                    : ""));
        }

        private String formatRegisterContent(String content) {
            if (content == null) return "";
            String formatted = content.replace("\n", "⏎").replace("<", "&lt;").replace(">", "&gt;");
            return formatted.length() > 70 ? formatted.substring(0, 70) + "..." : formatted;
        }
    }
}
