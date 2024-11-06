package com.julienphalip.ideavim.peekaboo;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.OperatorArguments;
import com.maddyhome.idea.vim.extension.ExtensionHandler;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.maddyhome.idea.vim.extension.VimExtensionFacade;
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

    @Override
    public String getName() {
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

    private static class ShowRegistersHandler implements ExtensionHandler {
        private final List<KeyStroke> originalKeyStrokes;

        public ShowRegistersHandler(List<KeyStroke> originalKeyStrokes) {
            this.originalKeyStrokes = originalKeyStrokes;
        }

        @Override
        public void execute(
                @NotNull VimEditor vimEditor,
                @NotNull ExecutionContext context,
                @NotNull OperatorArguments operatorArguments) {
            Editor editor =
                    com.intellij
                            .openapi
                            .fileEditor
                            .FileEditorManager
                            .getInstance(
                                    com.intellij.openapi.project.ProjectManager.getInstance()
                                            .getOpenProjects()[0])
                            .getSelectedTextEditor();
            if (editor != null) {
                // Re-execute the original key
                VimExtensionFacade.executeNormalWithoutMapping(originalKeyStrokes, editor);
                // Show the popup
                showPopup();
            }
        }

        private void showPopup() {
            StringBuilder html =
                    new StringBuilder(
                            """
<html>
<body style='margin: 3px; width: 100%; background-color: #2b2b2b; color: #A9B7C6;'>
<div style='font-family: monospace; min-width: 600px;'>
""");

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
            html.append("<div style='margin-bottom: 8px; color: #FFC66D;'>Special Registers</div>");
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
            html.append("<div style='margin-bottom: 8px; color: #FFC66D;'>Named Registers</div>");
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
            html.append("<div style='margin-bottom: 8px; color: #FFC66D;'>Last Yank</div>");
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
                    "<div style='margin-bottom: 8px; color: #FFC66D;'>Delete/Change History <span"
                            + " style='color: #808080;'>(deleted/changed content larger than one"
                            + " line)</span></div>");
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

            Balloon popup =
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder(
                                    html.toString(), null, new Color(43, 43, 43), null)
                            .setAnimationCycle(10)
                            .setHideOnClickOutside(true)
                            .setHideOnKeyOutside(true)
                            .createBalloon();

            JFrame ideFrame = WindowManager.getInstance().findVisibleFrame();
            if (ideFrame != null) {
                Point location = new Point(0, ideFrame.getHeight() - 100);
                RelativePoint point = new RelativePoint(ideFrame, location);
                popup.show(point, Balloon.Position.above);
            }
        }

        private void appendRegister(
                StringBuilder html, String register, String content, String description) {
            html.append(
                    String.format(
                            """
                            <div style='display: flex; margin-bottom: 4px; align-items: baseline;'>
                                <span style='color: #FFFFFF; font-weight: bold;'>%s</span>
                                <span style='color: #808080;'> → </span>
                                <span style='color: #A9B7C6;'>%s</span>
                                %s
                            </div>
                            """,
                            register,
                            formatRegisterContent(content),
                            description != null
                                    ? String.format(
                                            "<span style='color: #808080; margin-left:"
                                                    + " 8px;'>(%s)</span>",
                                            description)
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
