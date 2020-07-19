package com.mdu.minesweeper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController

/**
 * MenuFragment.kt
 * Class that contains main menu
 * @author Mike Du
 * @since July 2020
 */
class MenuFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Sends chosen difficulty as a integer via NavArgs
        view.findViewById<Button>(R.id.button_easy).setOnClickListener {
            val action = MenuFragmentDirections.actionMenuFragmentToGameFragment(1)
            findNavController().navigate(action)
        }
        view.findViewById<Button>(R.id.button_medium).setOnClickListener {
            val action = MenuFragmentDirections.actionMenuFragmentToGameFragment(2)
            findNavController().navigate(action)
        }
        view.findViewById<Button>(R.id.button_hard).setOnClickListener {
            val action = MenuFragmentDirections.actionMenuFragmentToGameFragment(3)
            findNavController().navigate(action)
        }
    }
}
