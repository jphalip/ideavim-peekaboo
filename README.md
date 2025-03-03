<!-- Plugin description -->
# Peekaboo (IdeaVim extension)

An IdeaVim extension that shows register contents in a popup window when
pressing `"` in normal mode or `Ctrl-R` in insert mode.

This extension is inspired from the [vim-peekaboo](https://github.com/junegunn/vim-peekaboo) plugin.

![Screenshot showing register contents](images/screenshot.png)

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
      for "Vim-Peekaboo"
    - Or download
      from [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/25776-vim-peekaboo)
3. Add `set peekaboo` to your `~/.ideavimrc` file, then run `:source ~/.ideavimrc` or restart the IDE.

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
