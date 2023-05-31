package com.wangjingbo.low.Routh

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.wangjingbo.low.R
import java.io.File

class FileListAdapter(
    context: Context,
    resource: Int,
    objects: List<File>
) : ArrayAdapter<File>(context, resource, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false)

        val fileTextView = view.findViewById<TextView>(R.id.fileTextView)
        val fileSizeTextView = view.findViewById<TextView>(R.id.fileSizeTextView)

        val file = getItem(position)
        fileTextView.text = file?.name
        fileSizeTextView.text = getFileSize(file)

        return view
    }

    private fun getFileSize(file: File?): String {
        if (file == null) return ""

        val fileSize = file.length()
        val kb = fileSize / 1024
        val mb = kb / 1024

        return when {
            mb > 0 -> "${mb}MB"
            kb > 0 -> "${kb}KB"
            else -> "${fileSize}B"
        }
    }
}