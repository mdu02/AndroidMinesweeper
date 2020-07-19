package com.mdu.minesweeper

import android.util.Log

/**
 * Board.kt
 * Class that contains exclusively game logic
 * @author Mike Du
 * @since July 2020
 */
class Board (val height: Int, val width: Int, mines: Int) {
    //board function
    var flagsLeft = mines
    var startTime = System.currentTimeMillis()

    private var minesLeft = mines
    private var squaresLeft = height*width
    private var firstTurn = true
    private val emptySquares = squaresLeft - minesLeft
    private val boardArray : Array<Array<Cell>> = Array(height) {Array(width) {Cell(mineGenHelp())}}

    //Check around a cell, addressed from top left corner
    val Y_OFFSETS = arrayOf(-1, -1, -1, 0, 1, 1, 1, 0)
    val X_OFFSETS = arrayOf(-1, 0, 1, 1, 1, 0, -1, -1)
    init {
        calculateNumbers()
        //debugBoard()
    }
    /**
     * External getter for cell number based on x and y coordinate
     * @param y Y-coordinate
     * @param x X-coordinate
     * @return cell number
     */
    fun check(y: Int, x: Int) : Int{
        return boardArray[y][x].value
    }

    /**
     * External getter for visibility status based on x and y coordinate
     * @param y Y-coordinate
     * @param x X-coordinate
     * @return whether the cell is visible
     */
    fun vis(y: Int, x: Int) : Boolean{
        return boardArray[y][x].visible
    }

    /**
     * External getter for flag status based on x and y coordinate
     * @param y Y-coordinate
     * @param x X-coordinate
     * @return whether a flag is present
     */
    fun flag(y: Int, x: Int) : Boolean{
        return boardArray[y][x].flag
    }

    /**
     * togFlag
     * Toggles flags
     * @param y Y-coordinate
     * @param x X-coordinate
     */
    fun togFlag(y: Int, x: Int){
        if (!this.vis(y,x)) { //toggle flag. Only if square not visible
            boardArray[y][x].flag = !boardArray[y][x].flag
            flagsLeft += if (this.flag(y,x)) -1 else 1
        }
    }

    /**
     * poolExpand
     * Expands all surrounding cells, when empty square is tapped and equal to 0
     * @param y Y-coordinate
     * @param x X-coordinate
     */
    fun poolExpand(y: Int, x: Int) {
        if (firstTurn){
            startTime = System.currentTimeMillis() //keep this for time tracking
            firstTurn = false
            if (boardArray[y][x].isMine) { //first opening could be mine, so we switch mine with top left
                var j = 0
                while (boardArray[y][x].isMine) {
                    if (!boardArray[0][j].isMine) {
                        boardArray[y][x].isMine = false
                        boardArray[0][j].isMine = true
                    }
                    j++
                }
                calculateNumbers() //recalculate numbers because of shift
            }
        }
        boardArray[y][x].visible = true
        if (boardArray[y][x].value == 0) {
            for (i in 0..7) {
                var yo = y + Y_OFFSETS[i]
                var xo = x + X_OFFSETS[i]
                if (yo in 0 until height && xo in 0 until width) {
                    if (!boardArray[yo][xo].visible && !boardArray[yo][xo].flag) { //expand only non-flagged non-visible cells
                        boardArray[yo][xo].visible = true
                        poolExpand(yo, xo)
                    }
                }
            }
        }
    }

    /**
     * dTapExpand
     * Expands all surrounding cells, when number is reached
     * @param y Y-coordinate
     * @param x X-coordinate
     */
    fun dTapExpand(y: Int, x: Int) {
        var flagSurrounding = 0
        for (i in 0..7){
            var yo = y + Y_OFFSETS[i]
            var xo = x + X_OFFSETS[i]
            if (yo in 0 until height && xo in 0 until width){
                if (boardArray[yo][xo].flag){
                    flagSurrounding++
                }
            }
        }
        if (flagSurrounding==check(y,x)) { //only open if square and surrounding match
            for (i in 0..7) {
                var yo = y + Y_OFFSETS[i]
                var xo = x + X_OFFSETS[i]
                if (yo in 0 until height && xo in 0 until width) {
                    if (!boardArray[yo][xo].visible && !boardArray[yo][xo].flag) {//expand only non-flagged non-visible cells
                        boardArray[yo][xo].visible = true
                        if (boardArray[yo][xo].value == 0) {
                            poolExpand(yo, xo)
                        }
                    }
                }
            }
        }
    }

    /**
     * gameOverCheck
     * Checks if game is over
     * @return 0 for in progress, +1 for win, -1 for loss
     */
    fun gameOverCheck(): Int{
        var clearCount = 0 //number of cleared squares
        for (i in 0 until height){
            for (j in 0 until width){
                if (boardArray[i][j].visible){
                    when (boardArray[i][j].isMine){
                        true -> return -1 //if any mines are visible
                        false -> clearCount++ //if anything else is visible
                    }
                }
            }
        }
        if (clearCount == emptySquares){
            return 1
        }
        return 0
    }

    /**
     * calculateNumbers
     * Helper function which assigns each cell a number based on surrounding mines
     */
    private fun calculateNumbers(){
        for (i in 0 until height){
            for (j in 0 until width){
                if (boardArray[i][j].isMine){
                    boardArray[i][j].value = -1
                } else {
                    var count = 0
                    for (k in 0..7){
                        val yo = i + Y_OFFSETS[k]
                        val xo = j + X_OFFSETS[k]
                        if (yo in 0 until height && xo in 0 until width){
                            if (boardArray[yo][xo].isMine){
                                count++
                            }
                        }
                    }
                    boardArray[i][j].value = count
                }
            }
        }
    }

    /**
     * Cell
     * Helper class to handle individual tiles. Properties self-explanatory
     */
    inner class Cell(var isMine: Boolean){
        var value = 0
        var visible = false
        var flag = false
    }

    /**
     * mineGenHelp
     * Helper function which determines if a mine should be placed
     * @return whether the next cell should be a mine or not, based on RNG
     */
    private fun mineGenHelp() : Boolean{
        if (Math.random() < minesLeft.toDouble()/squaresLeft--){
            minesLeft-- //decrement if mine has been chosen
            return true
        }
        return false
    }

    /**
     * debugBoard
     * Displays a neatly formatted board, where mines are -1 and other squares have their value
     */
    private fun debugBoard(){
        var out = "\n"
        for (i in 0 until height){
            for (j in 0 until width){
                out += if (boardArray[i][j].value >= 0) " " + boardArray[i][j].value.toString() else boardArray[i][j].value.toString()
            }
            out += '\n'
        }
        Log.d("Board Debug", out)
    }
}