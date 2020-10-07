package co.id.mnotesapp

import android.view.View

class CustomOnItemClickListener(private val position: Int, private val onItemCallback: OnItemCallback) : View.OnClickListener {

    override fun onClick(view: View) {
        onItemCallback.onItemClicked(view, position)
    }

    interface OnItemCallback {
        fun onItemClicked(view: View, position: Int)
    }
}