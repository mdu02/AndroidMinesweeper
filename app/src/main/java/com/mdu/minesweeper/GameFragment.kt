package com.mdu.minesweeper


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.*

/**
 * Fragment.kt
 * Class handling the game
 * @author Mike Du
 * @since July 2020
 */
class GameFragment : Fragment(), SurfaceHolder.Callback, View.OnTouchListener {
    lateinit var b : Board
    lateinit var bt: BoardThread

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    val args: GameFragmentArgs by navArgs() //passed difficulty

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val v = view.findViewById(R.id.gameSurface) as SurfaceView
        v.holder.addCallback(this)
        view.findViewById<Button>(R.id.button_return_menu).setOnClickListener {
            findNavController().navigate(R.id.action_GameFragment_to_MenuFragment)
        }
        view.findViewById<Button>(R.id.button_restart_game).setOnClickListener {
            bt.reset()
        }
        v.setOnTouchListener(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        when (args.difficultyArg){ //these are the defaults
            1 -> b = Board(8,8,10)
            2 -> b = Board(16,16,40)
            3 -> b = Board(30, 16, 99)
        }
        bt = BoardThread(holder, b)
        bt.start()
        textCoroutine(bt,view)
    }

    /**
     * textCoroutine
     * A coroutine that checks and updates game information for the UI
     * @param boardThread The thread which runs the game logic
     * @param view The fragment view
     */
    private fun textCoroutine(boardThread: BoardThread, view: View?) {
        GlobalScope.launch(Dispatchers.Main) {//start coroutine
            while (true) {
                if (bt.running) { //always running while fragment is active, but only checks while game in progress
                    val flagsLeft = boardThread.board.flagsLeft
                    val flagsLeftString = "Mines Left: $flagsLeft"
                    val timeElapsed = (System.currentTimeMillis() - boardThread.board.startTime) / 1000
                    val timeElapsedString = "Time: $timeElapsed"
                    view?.findViewById<TextView>(R.id.mines_left)?.text = flagsLeftString
                    view?.findViewById<TextView>(R.id.game_time)?.text = timeElapsedString
                }
                delay(500) // wait half a second
            }
        }
    }
    override fun onPause() {
        super.onPause()
        bt.pause()
    }

    private var lastClick :Long = 0 //keeps track of time
    override fun onTouch(v: View?, m: MotionEvent): Boolean {
        val tX = m.x
        val tY = m.y
        if (bt.running) {
            when (m.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (m.eventTime - lastClick < 100) { //two clicks in short succesion
                        b.dTapExpand(
                            ((tY - bt.yOffset) / bt.squareSide).toInt(),
                            ((tX - bt.xOffset) / bt.squareSide).toInt()
                        )
                    }
                    lastClick = m.eventTime
                }
                MotionEvent.ACTION_UP -> {
                    if (m.eventTime - lastClick > 150) { //long tap
                        b.poolExpand(
                            ((tY - bt.yOffset) / bt.squareSide).toInt(),
                            ((tX - bt.xOffset) / bt.squareSide).toInt()
                        )
                    } else { //regular tap
                        b.togFlag(
                            ((tY - bt.yOffset) / bt.squareSide).toInt(),
                            ((tX - bt.xOffset) / bt.squareSide).toInt()
                        )
                    }
                }
            }
            when (b.gameOverCheck()) { //check game over after each event
                1 -> bt.gameEndDisplay(1)
                -1 -> bt.gameEndDisplay(-1)

            }
            lastClick = m.eventTime
        }
        return true
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }

    /**
     * BoardThread
     * Thread running game rendering and logic
     * @param holder Is a holder for SurfaceView
     * @param board An object that contains game logic
     */
    inner class BoardThread(private val holder: SurfaceHolder, var board: Board) : Thread() {
        var sprites: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.tilesprites)
        var running = true //if game is active
        var c = holder.lockCanvas()
        val squareSide = Math.min(c.height/board.height, c.width/board.width) //scales board to fit into the screen
        val xOffset = (c.width - (squareSide*board.width))/2
        val yOffset = (c.height - (squareSide*board.height))/2
        init {
            sprites = Bitmap.createScaledBitmap(sprites, squareSide*11, squareSide, true)
            holder.unlockCanvasAndPost(c)
        }

        override fun run() {
            while(true){
                if (running) {
                    if (!holder.surface.isValid) {
                        continue
                    }
                    c = holder.lockCanvas()
                    c.drawARGB(250, 191, 191, 191)
                    drawLines()
                    drawSprites()
                    holder.unlockCanvasAndPost(c)
                }
                else {
                    continue
                }
            }
        }

        fun pause(){
            running = false
        }

        /**
         * drawSprites
         * A function which loops through the entire board and draws each cell
         */
        private fun drawSprites(){
            for (i in 0 until board.height){
                for (j in 0 until board.width){
                    val bmDestination = Rect(j*squareSide, i*squareSide, (j+1)*squareSide, (i+1)*squareSide)
                    bmDestination.offset(xOffset, yOffset)
                    val bmFrom = if (board.flag(i,j)) Rect(0,0,squareSide, squareSide) else if (!board.vis(i,j)) Rect(0,0,0,0) else
                        when (val tileValue = board.check(i,j)){
                            -1 -> Rect(9*squareSide,0,10*squareSide,squareSide)
                            0 -> Rect(10*squareSide,0,11*squareSide,squareSide)
                            in 1..8 -> Rect(tileValue*squareSide, 0, (tileValue+1)*squareSide, squareSide)
                            else -> Rect(0,0,0,0)
                        }
                    c.drawBitmap(sprites, bmFrom, bmDestination, null)
                }
            }
        }

        /**
         * drawLines
         * Draws gridlines on display
         */
        private fun drawLines(){
            val p = Paint()
            p.strokeWidth = 3F
            for (i in 0 .. board.height){
                c.drawLine(
                    xOffset.toFloat(),
                    (i*squareSide + yOffset).toFloat(),
                    (board.width*squareSide + xOffset).toFloat(),
                    (i*squareSide + yOffset).toFloat(), p)
            }
            for (j in 0 .. board.width){
                c.drawLine(
                    (j*squareSide + xOffset).toFloat(),
                    yOffset.toFloat(),
                    (j*squareSide + xOffset).toFloat(),
                    (board.height*squareSide + yOffset).toFloat(), p)
            }
        }

        /**
         * gameEndDisplay
         * Displays text for win or loss
         * @param code Game State (1 = win, -1 = loss. Int because 0 is a possibility upstream)
         */
        fun gameEndDisplay(code: Int){
            running = false //stop game
            c = holder.lockCanvas()
            val p = Paint()
            p.textSize = 200F
            when (code){
                1 -> (c.drawText("You Won!", xOffset.toFloat(), yOffset.toFloat(), p))
                -1 ->(c.drawText("You Lost", xOffset.toFloat(), yOffset.toFloat(), p))
            }
            holder.unlockCanvasAndPost(c)
            val restart = view?.findViewById<Button>(R.id.button_restart_game)
            restart?.visibility = View.VISIBLE //Makes restart button visible to player
        }

        fun reset(){
            val restart = view?.findViewById<Button>(R.id.button_restart_game)
            restart?.visibility = View.INVISIBLE
            when (args.difficultyArg){
                1 -> b = Board(8,8,10)
                2 -> b = Board(16,16,40)
                3 -> b = Board(30, 16, 99)
            }
            board = b
            running = true
        }

    }
}