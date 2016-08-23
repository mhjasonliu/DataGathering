package com.northwestern.habits.datagathering.userinterface;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.notifications.MessageFlags;
import com.microsoft.band.notifications.VibrationType;
import com.microsoft.band.tiles.BandTile;
import com.microsoft.band.tiles.TileButtonEvent;
import com.microsoft.band.tiles.TileEvent;
import com.microsoft.band.tiles.pages.FlowPanel;
import com.microsoft.band.tiles.pages.FlowPanelOrientation;
import com.microsoft.band.tiles.pages.PageData;
import com.microsoft.band.tiles.pages.PageLayout;
import com.microsoft.band.tiles.pages.ScrollFlowPanel;
import com.microsoft.band.tiles.pages.TextBlock;
import com.microsoft.band.tiles.pages.TextBlockData;
import com.microsoft.band.tiles.pages.TextBlockFont;
import com.microsoft.band.tiles.pages.TextButton;
import com.microsoft.band.tiles.pages.TextButtonData;
import com.northwestern.habits.datagathering.R;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TileActivity extends Activity {
    private BandClient client = null;
    private Button btnStart;
    private TextView txtStatus;

    private static final UUID tileId = UUID.fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
    private static final UUID pageId1 = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd00");
    private static final UUID pageId2 = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd01");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tile);

        txtStatus = (TextView) findViewById(R.id.txtStatus);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtStatus.setText("");
                new appTask().execute();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TileEvent.ACTION_TILE_OPENED);
        filter.addAction(TileEvent.ACTION_TILE_BUTTON_PRESSED);
        filter.addAction(TileEvent.ACTION_TILE_CLOSED);
        registerReceiver(messageReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }


    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == TileEvent.ACTION_TILE_OPENED) {
            } else if (intent.getAction() == TileEvent.ACTION_TILE_CLOSED) {
            }

            /* ***** THIS IS THE ONLY EVENT WE ACTUALLY CARE ABOUT ***** */
            else if (intent.getAction() == TileEvent.ACTION_TILE_BUTTON_PRESSED) {
                TileButtonEvent buttonData = intent.getParcelableExtra(TileEvent.TILE_EVENT_DATA);
                try {
                    onButtonClicked(buttonData.getElementID());
                } catch (BandIOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.append(string);
            }
        });
    }

    /* *************************** DIALOG STUFF *************************** */

    public void sendDialog(View view) {
        new SendDialogTask().execute();
    }

    private class SendDialogTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
//                    client.getNotificationManager().showDialog(tileId, "Dialog", "This is a dialog!").await();
                    while (true) {
                        client.getNotificationManager().vibrate(VibrationType.NOTIFICATION_ALARM).await();
                    }
                }
            } catch (BandException | InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    /* *************************** MESSAGE STUFF *************************** */
    public void sendMessage(View view) {
        new SendMessageTask().execute();
    }

    private class SendMessageTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {

                if (getConnectedBandClient()) {
                    client.getNotificationManager().sendMessage(tileId, "Message title",
                            "This is the message body!", new Date(), MessageFlags.SHOW_DIALOG).await();
                }

            } catch (BandException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(getBaseContext(), "Message sent to band", Toast.LENGTH_SHORT).show();
        }
    }





    /* *************************** TILE STUFF *************************** */

    private boolean doesTileExist(List<BandTile> tiles, UUID tileId) {
        for (BandTile tile : tiles) {
            if (tile.getTileId().equals(tileId)) {
                return true;
            }
        }
        return false;
    }

    private class appTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    if (addTile()) {
                        updatePages();
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage = "";
                switch (e.getErrorType()) {
                    case DEVICE_ERROR:
                        exceptionMessage = "Please make sure bluetooth is on and the band is in range.\n";
                        break;
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    case BAND_FULL_ERROR:
                        exceptionMessage = "Band is full. Please use Microsoft Health to remove a tile.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private boolean addTile() throws Exception {
        if (doesTileExist(client.getTileManager().getTiles().await(), tileId)) {
            client.getTileManager().removeTile(tileId);
        }

		/* Set the options */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap tileIcon = BitmapFactory.decodeResource(getBaseContext().getResources(), R.drawable.hamburger, options);

        BandTile tile = new BandTile.Builder(tileId, "Button Tile", tileIcon)
                .setPageLayouts(createButtonLayout(), createTextLayout())
                .build();
        appendToUI("Button Tile is adding ...\n");
        if (client.getTileManager().addTile(this, tile).await()) {
            appendToUI("Button Tile is added.\n");
            return true;
        } else {
            appendToUI("Unable to add button tile to the band.\n");
            return false;
        }
    }


    private final int LAYOUT_TEXT = 11;
    private final int TXT_TITLE = 21;
    private final int TXT_ACTIVITY = 22;

    private PageLayout createTextLayout() {
        return new PageLayout(
                new FlowPanel(15, 0, 260, 125, FlowPanelOrientation.VERTICAL)
                        .addElements(new TextBlock(0, 0, 260, 45, TextBlockFont.MEDIUM).setMargins(0, 5, 0, 0)
                                .setId(TXT_TITLE).setAutoWidthEnabled(true))
                        .addElements(new TextBlock(0, 0, 260, 90, TextBlockFont.SMALL).setMargins(0, 5, 0, 0)
                                .setId(TXT_ACTIVITY).setAutoWidthEnabled(true)));
    }

    private final int LAYOUT_BUTTONS = 12;
    private final int BTN_EATING = 31;
    private final int BTN_DRINKING = 32;
    private final int BTN_NOTHING = 33;

    private PageLayout createButtonLayout() {
        return new PageLayout(
                new ScrollFlowPanel(15, 0, 260, 125, FlowPanelOrientation.VERTICAL)
                        .addElements(new TextButton(0, 0, 260, 45).setMargins(0, 5, 0, 0)
                                .setId(BTN_EATING).setPressedColor(Color.BLUE))
                        .addElements(new TextButton(0, 0, 260, 45).setMargins(0, 5, 0, 0)
                                .setId(BTN_DRINKING).setPressedColor(Color.BLUE))
                        .addElements(new TextButton(0, 0, 260, 45).setMargins(0, 5, 0, 0)
                                .setId(BTN_NOTHING).setPressedColor(Color.BLUE))
        );
    }

    private String activity = "Nothing";

    private void updatePages() throws BandIOException {
        client.getTileManager().setPages(tileId,
                new PageData(pageId2, 0)
                        .update(new TextButtonData(BTN_EATING, "Eating"))
                        .update(new TextButtonData(BTN_DRINKING, "Drinking"))
                        .update(new TextButtonData(BTN_NOTHING, "Note to self")),
                new PageData(pageId1, 1)
                        .update(new TextBlockData(TXT_TITLE, "Current Activity"))
                        .update(new TextBlockData(TXT_ACTIVITY, activity)));
    }

    private void onButtonClicked(int clickedID) throws BandIOException {
        String prev = activity;
        switch (clickedID) {
            case BTN_EATING:
                activity = "Eating";
                break;
            case BTN_DRINKING:
                activity = "Drinking";
                break;
            case BTN_NOTHING:
                activity = "Note to self";
                break;
            default:
                Log.e("", "Unknown button press received");
        }
        if (!Objects.equals(activity, prev)) {
            updatePages();
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(10, 10, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }



    /* *************************** GENERIC BAND STUFF *************************** */

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }
}
