package com.karan.labeldetection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;

/**
 * Created by stpl on 3/24/2017.
 */

public class AlertDialogManager {
    /**
     * Function to display simple Alert Dialog
     * @param context - application context
     * @param title - alert dialog title
     * @param message - alert message
     * @param status - success/failure (used to set icon)
     *               - pass null if you don't want icon
     * */
    public void showAlertDialog(final Context context, String title, String message,
                                Boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setCanceledOnTouchOutside(false);

        // Setting Dialog Title
        alertDialog.setTitle(title);

        // Setting Dialog Message
        alertDialog.setMessage(message);


        alertDialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    ((Activity)context).finish();
                    alertDialog.dismiss();
                }
                return true;
            }
        });


        if(status != null)
            // Setting alert dialog icon
//            alertDialog.setIcon((status) ? R.drawable.success : R.drawable.fail);

            // Setting OK Button
            alertDialog.setButton("OK", (dialog, which) -> ((Activity)context).finish());

        // Showing Alert Message
        alertDialog.show();
    }
}