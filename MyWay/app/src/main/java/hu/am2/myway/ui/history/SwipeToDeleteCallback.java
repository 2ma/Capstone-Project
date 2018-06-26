package hu.am2.myway.ui.history;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import hu.am2.myway.R;

//based on: https://medium.com/@kitek/recyclerview-swipe-to-delete-easier-than-you-thought-cff67ff5e5f6
public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final ColorDrawable background = new ColorDrawable();
    private final Drawable icon;
    private final int intrinsicWidth;
    private final int intrinsicHeight;

    SwipeToDeleteCallback(Context context) {
        super(0, ItemTouchHelper.LEFT);
        int backColor = ContextCompat.getColor(context, R.color.delete_red);
        icon = ContextCompat.getDrawable(context, R.drawable.ic_delete_24dp);
        intrinsicWidth = icon.getIntrinsicWidth();
        intrinsicHeight = icon.getIntrinsicHeight();
        background.setColor(backColor);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean
        isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        int height = itemView.getBottom() - itemView.getTop();
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            background.draw(c);
            int iconMargin = (height - intrinsicHeight) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + intrinsicHeight;
            icon.setBounds(itemView.getRight() - iconMargin - intrinsicWidth, iconTop, itemView.getRight() - iconMargin, iconBottom);
            icon.draw(c);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
