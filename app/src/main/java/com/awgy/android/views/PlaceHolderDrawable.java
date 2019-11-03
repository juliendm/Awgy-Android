package com.awgy.android.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.awgy.android.R;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.utils.Constants;

public class PlaceHolderDrawable extends Drawable {

    private Context mContext;
    private GroupSelfie mGroupSelfie;

    public PlaceHolderDrawable(Context context, GroupSelfie groupSelfie) {
        mContext = context;
        mGroupSelfie = groupSelfie;
    }

    @Override
    public void draw(Canvas canvas) {

        if (mGroupSelfie.getDesc() != null) {

            // Description

            String[] lines = mGroupSelfie.getDesc().split("/");

            int n_column = 0;
            for (String line : lines) {
                int local_n_column = 0;
                for (String cell : line.split("\\.")) {
                    if (cell.length() > 0) local_n_column += Integer.parseInt(cell.substring(cell.length() - 1));
                }
                if (local_n_column > n_column) n_column = local_n_column;
            }
            int width = Constants.IMAGE_BORDER + n_column * (Constants.IMAGE_BORDER + Constants.IMAGE_CANVAS);

            // Pic

            Paint pic = new Paint(Paint.ANTI_ALIAS_FLAG);
            pic.setStyle(Paint.Style.FILL);

            // Dimensions

            int placeholder_width;
            int placeholder_height;

            double ratio = mGroupSelfie.getImageRatio();
            if ((double) canvas.getWidth() / (double) canvas.getHeight() < ratio) {
                placeholder_width = canvas.getWidth();
                placeholder_height = (int) Math.ceil(placeholder_width / ratio);
            } else {
                placeholder_height = canvas.getHeight();
                placeholder_width = (int) Math.ceil(placeholder_height * ratio);
            }

            if (n_column == 1) {
                placeholder_width = (int)Math.ceil((2.0*Constants.IMAGE_BORDER+Constants.IMAGE_CANVAS)/(3.0*Constants.IMAGE_BORDER+2.0*Constants.IMAGE_CANVAS)*canvas.getWidth());
                placeholder_height = placeholder_width;
            }

            double scale = (double)placeholder_width/(double)width;

            int image_border = (int)Math.ceil(Constants.IMAGE_BORDER*scale);
            int image_canvas = (int)Math.ceil(Constants.IMAGE_CANVAS*scale);

            // Drawing

            int origin_x = (int) Math.ceil(0.5 * (canvas.getWidth() - placeholder_width));

            int origin_y = (int) Math.ceil(0.5 * (canvas.getHeight() - placeholder_height));

            int current_x;
            int current_y = origin_y + image_border;

            for (String line : lines) {

                current_x = origin_x + image_border;

                String[] cells = line.split("\\.");

                int local_n_column = 0;
                for (String cell : cells) if (cell.length() > 0) local_n_column += Integer.parseInt(cell.substring(cell.length()-1));
                if (local_n_column < n_column) current_x += (int)Math.ceil(0.5*image_canvas);

                for (String cell : cells) {

                    if (cell.length() > 0) {

                        int dim = Integer.parseInt(cell.substring(cell.length()-1));

                        if (dim > 0) {

                            String colorHex = cell.substring(0, cell.length()-1);
                            if (colorHex.length() == 6) {
                                pic.setColor(Color.parseColor(String.format("#%s",colorHex)));
                            } else {
                                pic.setColor(mContext.getResources().getColor(R.color.color2));
                            }

                            RectF rectF = new RectF();

                            rectF.set(current_x,
                                    current_y,
                                    current_x + dim * image_canvas + (dim - 1) * image_border,
                                    current_y + image_canvas);
                            canvas.drawRect(rectF, pic);

                            current_x += dim * (image_border + image_canvas);

                        }

                    }
                }

                current_y += image_border + image_canvas;

            }

        }

    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }
}
