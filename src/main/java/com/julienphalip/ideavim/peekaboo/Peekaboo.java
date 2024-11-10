package com.julienphalip.ideavim.peekaboo;

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
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.OperatorArguments;
import com.maddyhome.idea.vim.extension.ExtensionHandler;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.maddyhome.idea.vim.key.KeyMapping;
import com.maddyhome.idea.vim.key.MappingInfo;
import com.maddyhome.idea.vim.register.Register;
import java.awt.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import javax.swing.*;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

public class Peekaboo implements VimExtension {

    private static final String REGISTER_COLOR = "#FFFFFF";
    private static final String BG_COLOR = "#2b2b2b";
    private static final String TEXT_COLOR = "#A9B7C6";
    private static final String HEADER_COLOR = "#FFC66D";
    private static final String COMMENT_COLOR = "#808080";

    @Override
    public @NotNull String getName() {
        return "peekaboo";
    }

    @Override
    public void init() {
        // Register mapping for quote character in normal mode
        List<KeyStroke> quoteKeyStroke = Collections.singletonList(KeyStroke.getKeyStroke('"'));
        KeyMapping normalModeKeyMapping = VimPlugin.getKey().getKeyMapping(MappingMode.NORMAL);
        List<Pair<List<KeyStroke>, MappingInfo>> quoteMappings =
                normalModeKeyMapping.getMapTo(quoteKeyStroke);
        for (Pair<List<KeyStroke>, MappingInfo> mapping : quoteMappings) {
            VimPlugin.getKey()
                    .putKeyMapping(
                            EnumSet.of(MappingMode.NORMAL),
                            mapping.getSecond().getFromKeys(),
                            getOwner(),
                            new ShowRegistersHandler(quoteKeyStroke),
                            false);
        }
        if (normalModeKeyMapping.get(quoteKeyStroke) == null) {
            VimPlugin.getKey()
                    .putKeyMapping(
                            EnumSet.of(MappingMode.NORMAL),
                            quoteKeyStroke,
                            getOwner(),
                            new ShowRegistersHandler(quoteKeyStroke),
                            false);
        }
        // Register mapping for control-r in insert mode
        List<KeyStroke> controlRKeyStroke =
                Collections.singletonList(KeyStroke.getKeyStroke("control R"));
        KeyMapping insertModeKeyMapping = VimPlugin.getKey().getKeyMapping(MappingMode.INSERT);
        List<Pair<List<KeyStroke>, MappingInfo>> controlRMappings =
                insertModeKeyMapping.getMapTo(controlRKeyStroke);
        for (Pair<List<KeyStroke>, MappingInfo> mapping : controlRMappings) {
            VimPlugin.getKey()
                    .putKeyMapping(
                            EnumSet.of(MappingMode.INSERT),
                            mapping.getSecond().getFromKeys(),
                            getOwner(),
                            new ShowRegistersHandler(controlRKeyStroke),
                            false);
        }
        if (insertModeKeyMapping.get(controlRKeyStroke) == null) {
            VimPlugin.getKey()
                    .putKeyMapping(
                            EnumSet.of(MappingMode.INSERT),
                            controlRKeyStroke,
                            getOwner(),
                            new ShowRegistersHandler(controlRKeyStroke),
                            false);
        }
    }

    private record ShowRegistersHandler(List<KeyStroke> originalKeyStrokes)
            implements ExtensionHandler {

        @Override
        public void execute(
                @NotNull VimEditor vimEditor,
                @NotNull ExecutionContext context,
                @NotNull OperatorArguments operatorArguments) {
            // Re-execute the original key
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
            showPopup();
        }

        private void showPopup() {
            StringBuilder html =
                    new StringBuilder(
                            String.format(
                                    """
<html>
<body style='margin: 3px; width: 100%%; background-color: %s; color: %s;'>
<div style='font-family: monospace; min-width: 600px;'>
""",
                                    BG_COLOR, TEXT_COLOR));

            java.util.Map<String, String> registerDescriptions = new java.util.HashMap<>();
            registerDescriptions.put(
                    "\"", "unnamed register: last deleted, changed, or yanked content");
            registerDescriptions.put("-", "deleted or changed content smaller than one line");
            registerDescriptions.put(":", "last executed command");
            registerDescriptions.put(".", "last inserted text");
            registerDescriptions.put("%", "name of the current file");
            registerDescriptions.put("#", "name of the alternate file");
            registerDescriptions.put("=", "expression register");
            registerDescriptions.put("/", "last search pattern");
            registerDescriptions.put("*", "system clipboard");
            registerDescriptions.put("+", "system selection primary");

            // Add special registers first
            html.append("<div style='margin-bottom: 15px;'>");
            html.append(
                    String.format(
                            "<div style='margin-bottom: 8px; color: %s;'>Special Registers</div>",
                            HEADER_COLOR));
            String[] specialRegisters = {"\"", "*", "+", "%", "#", ".", ":", "/", "=", "-"};
            for (String reg : specialRegisters) {
                Optional<Register> register =
                        Optional.ofNullable(VimPlugin.getRegister().getRegister(reg.charAt(0)));
                register.ifPresent(
                        r -> {
                            String content = r.getText();
                            if (content != null && !content.isEmpty()) {
                                appendRegister(html, reg, content, registerDescriptions.get(reg));
                            }
                        });
            }
            html.append("</div>");

            // Add named registers (a-z)
            html.append("<div style='margin-bottom: 15px;'>");
            html.append(
                    String.format(
                            "<div style='margin-bottom: 8px; color: %s;'>Named Registers</div>",
                            HEADER_COLOR));
            for (char c = 'a'; c <= 'z'; c++) {
                Optional<Register> register =
                        Optional.ofNullable(VimPlugin.getRegister().getRegister(c));
                char finalC = c;
                register.ifPresent(
                        reg -> {
                            String content = reg.getText();
                            if (content != null && !content.isEmpty()) {
                                appendRegister(html, String.valueOf(finalC), content, null);
                            }
                        });
            }
            html.append("</div>");

            // Add Last Yank (register 0)
            html.append("<div style='margin-bottom: 15px;'>");
            html.append(
                    String.format(
                            "<div style='margin-bottom: 8px; color: %s;'>Last Yank</div>",
                            HEADER_COLOR));
            Optional<Register> register0 =
                    Optional.ofNullable(VimPlugin.getRegister().getRegister('0'));
            register0.ifPresent(
                    reg -> {
                        String content = reg.getText();
                        if (content != null && !content.isEmpty()) {
                            appendRegister(html, "0", content, null);
                        }
                    });
            html.append("</div>");

            // Add numbered registers (1-9) - Delete/Change history
            html.append("<div style='margin-bottom: 15px;'>");
            html.append(
                    String.format(
                            "<div style='margin-bottom: 8px; color: %s;'>Delete/Change History"
                                    + " <span style='margin-left: 8px; color: %s;'>(deleted/changed"
                                    + " content larger than one line)</span></div>",
                            HEADER_COLOR, COMMENT_COLOR));
            for (int i = 1; i <= 9; i++) {
                Optional<Register> register =
                        Optional.ofNullable(
                                VimPlugin.getRegister().getRegister(String.valueOf(i).charAt(0)));
                int finalI = i;
                register.ifPresent(
                        reg -> {
                            String content = reg.getText();
                            if (content != null && !content.isEmpty()) {
                                appendRegister(
                                        html,
                                        String.valueOf(finalI),
                                        content,
                                        finalI == 1 ? "most recent" : null);
                            }
                        });
            }
            html.append("</div>");

            html.append("</div></body></html>");

            Balloon balloon =
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder(html.toString(), null, Gray._43, null)
                            .setAnimationCycle(10)
                            .setHideOnClickOutside(true)
                            .setHideOnKeyOutside(true)
                            .createBalloon();

            Window mostRecentFocusedWindow =
                    WindowManager.getInstance().getMostRecentFocusedWindow();
            if (mostRecentFocusedWindow instanceof IdeFrame activeFrame) {
                Point location = new Point(0, activeFrame.getComponent().getHeight() - 100);
                RelativePoint point = new RelativePoint(activeFrame.getComponent(), location);
                balloon.show(point, Balloon.Position.above);
            }
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
                            REGISTER_COLOR,
                            register,
                            COMMENT_COLOR,
                            TEXT_COLOR,
                            formatRegisterContent(content),
                            description != null
                                    ? String.format(
                                            "<span style='color: %s; margin-left:"
                                                    + " 8px;'>(%s)</span>",
                                            COMMENT_COLOR, description)
                                    : ""));
        }

        private String formatRegisterContent(String content) {
            if (content == null) return "";
            String formatted = content.replace("\n", "⏎").replace("<", "&lt;").replace(">", "&gt;");
            if (formatted.length() > 70) {
                return formatted.substring(0, 70) + "...";
            }
            return formatted;
        }
    }
}
