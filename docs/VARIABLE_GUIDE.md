# GameManager terminology

The board stores pieces, directions describe movement, and winning
coordinates identify the positions that the view should highlight.

## Fields and constants

| Name | Meaning |
| --- | --- |
| `ROWS`, `COLUMNS` | Board dimensions: 6 and 7. |
| `CONNECT_LENGTH` | Four consecutive pieces are required to win. |
| `EMPTY` | A space character marking an unoccupied position. |
| `DIRECTIONS` | Four `{row change, column change}` movement pairs. |
| `board` | The `char[][]` containing X, O, or a space at each position. |
| `turn` | Next player; while checking a move, the player who just placed it. |
| `gameFinished` | Whether victory or a draw blocks further moves. |
| `movesPlayed` | Number of accepted moves in the current game. |
| `winningCells` | Empty initially; four verified `{row, column}` pairs after victory. |

## Parameters and local variables

| Name | Meaning in its method |
| --- | --- |
| `columnLetter` | The requested column A through G in `dropPiece`. |
| `rowIndex`, `colIndex` | Zero-based board coordinates. |
| `direction` | The movement pair currently selected from `DIRECTIONS`. |
| `rowStep`, `colStep` | How much each coordinate changes per forward step. |
| `offset` | How many positions to move backward from the latest piece to a candidate start. |
| `startRow`, `startCol` | Starting coordinates of that candidate. |
| `row`, `col` | Starting coordinates in line methods; coordinates to validate in `isInsideBoard`. |
| `step` | Distance from the start: 0, 1, 2, or 3. |
| `currentRow`, `currentCol` | Coordinates currently inspected in `isWinningLine`. |
| `copy` | Independent coordinates returned by `getWinningCells`. |
| `index` | The coordinate-pair index being copied. |

## One horizontal example

Starting at A1 means `{row = 0, col = 0}`. Moving right means
`{rowStep = 0, colStep = 1}`. For each step:

| Step | Row calculation | Column calculation | Position |
| --- | --- | --- | --- |
| 0 | `0 + 0 * 0` | `0 + 0 * 1` | A1 |
| 1 | `0 + 1 * 0` | `0 + 1 * 1` | B1 |
| 2 | `0 + 2 * 0` | `0 + 2 * 1` | C1 |
| 3 | `0 + 3 * 0` | `0 + 3 * 1` | D1 |

The calculation finds positions. Comparing each `board[row][col]` with the
player's symbol proves whether those positions form a winning line.
