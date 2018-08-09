
package com.atelieryl.wonderdroid.views;

import com.atelieryl.wonderdroid.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RomGalleryView extends LinearLayout {

    private final ImageView iv;

    private final TextView title;

    public RomGalleryView(Context context) {
        super(context);
        LayoutInflater layoutInflater = (LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout mLayout = (LinearLayout)((LinearLayout)layoutInflater.inflate(
                R.layout.romgalleyview, this)).getChildAt(0);
        iv = (ImageView)mLayout.getChildAt(1);
        title = ((TextView)mLayout.getChildAt(0));
    }

    public void setSnap(Bitmap bm) {
        if (bm != null)
            iv.setImageBitmap(bm);
        else
            iv.setImageResource(R.drawable.unknownrom);
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

}
