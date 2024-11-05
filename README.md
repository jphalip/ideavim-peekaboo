<!-- Plugin description -->
# IdeaVim-Peekaboo

An IdeaVim extension that shows register contents in a popup window when
pressing `"` in normal mode or `Ctrl-R` in insert mode.

![Screenshot showing register contents](screenshot.png)

This extension is inspired from the [vim-peekaboo](https://github.com/junegunn/vim-peekaboo) plugin.

## Features

- Shows register contents in a popup window
- Works with both normal mode `"` and insert mode `Ctrl-R`
- Respects your own key mappings (e.g., if you've remapped these keys)
- Organized sections:
    - Special registers (`"`, `*`, `+`, etc.)
    - Named registers (a-z)
    - Last yank (register 0)
    - Delete/Change history (registers 1-9)

## Installation

1. Install [IdeaVim](https://plugins.jetbrains.com/plugin/164-ideavim) if you
   haven't already
2. Install this plugin:
    - In IntelliJ IDEA: Settings/Preferences → Plugins → Marketplace → Search
      for "IdeaVim-Peekaboo"
    - Or download
      from [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/YOUR_PLUGIN_ID)
3. Add `set peekaboo` in your `~/.ideavimrc` file

## Usage

1. In normal mode: Press `"` to see registers before using them in commands
2. In insert mode: Press `Ctrl-R` to see registers before pasting their contents

The popup will show automatically and disappear once you:

- Select a register
- Press any other key
- Click outside the popup

## Customization

The plugin works with your own mappings. For example, if you have these in
your `~/.ideavimrc`:

```vim
" Alt-R instead of Ctrl-R in insert mode
inoremap <A-r> <C-r>
" & instead of " in normal mode
nnoremap & "
```

The popup will appear when you use your custom mappings too.
<!-- Plugin description end -->