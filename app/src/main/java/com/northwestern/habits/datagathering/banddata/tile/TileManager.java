package com.northwestern.habits.datagathering.banddata.tile;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandErrorType;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.notifications.VibrationType;
import com.microsoft.band.tiles.BandTile;
import com.microsoft.band.tiles.TileButtonEvent;
import com.microsoft.band.tiles.TileEvent;
import com.microsoft.band.tiles.pages.FlowPanelOrientation;
import com.microsoft.band.tiles.pages.PageData;
import com.microsoft.band.tiles.pages.PageLayout;
import com.microsoft.band.tiles.pages.ScrollFlowPanel;
import com.microsoft.band.tiles.pages.TextBlock;
import com.microsoft.band.tiles.pages.TextBlockData;
import com.microsoft.band.tiles.pages.TextBlockFont;
import com.microsoft.band.tiles.pages.TextButton;
import com.microsoft.band.tiles.pages.TextButtonData;
import com.northwestern.habits.datagathering.MyReceiver;
import com.northwestern.habits.datagathering.R;
import com.northwestern.habits.datagathering.database.DataManagementService;

import java.util.List;
import java.util.UUID;

/**
 * Created by William on 8/23/2016
 */
public class TileManager extends BroadcastReceiver {
    private static String activity = "Nothing";
    private static final String TAG = "TileManager";

    /**
     * Maps tile to a band
     */

    public TileManager() {
    }

    public void sendTile(Activity a, String macAddress) {
        BandInfo[] bands = BandClientManager.getInstance().getPairedBands();
        for (BandInfo b :
                bands) {
            if (b.getMacAddress().equals(macAddress)) {
                new appTask(b, a, a).execute();
                break;
            }
        }

    }

    static boolean onButtonClicked(int clickedID, Context context) throws BandIOException {
        String prev = activity;
        Intent i = new Intent(MyReceiver.ACTION_LABEL);
        switch (clickedID) {
            case BTN_EATING:
                activity = "Eating";
                i.putExtra(MyReceiver.LABEL_EXTRA, DataManagementService.L_EATING);
                break;
            case BTN_DRINKING:
                activity = "Drinking";
                i.putExtra(MyReceiver.LABEL_EXTRA, DataManagementService.L_DRINKING);
                break;
            case BTN_NOTHING:
                activity = "Note to self";
                i.putExtra(MyReceiver.LABEL_EXTRA, DataManagementService.L_NOTHING);
                break;
            default:
                Log.e("", "Unknown button press received");
        }
        context.sendBroadcast(i);
//        return !Objects.equals(activity, prev);
        return true;
    }


    /* *************************** TILE MANAGING FUNCTIONS *************************** */
//    private static final UUID tileId = UUID.fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
    private static final UUID pageId1 = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd00");
    private static final UUID pageId2 = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd01");

    private boolean addTile(BandInfo info, final Activity activity) throws Exception {
        final BandClient client = getConnectedBandClient(info, activity);
        UUID tileId = generateUUID(info.getMacAddress());

        if (doesTileExist(client.getTileManager().getTiles().await(), tileId)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(activity).setTitle("Send notification to band?")
                            .setMessage("Click ok to make this band vibrate")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        client.getNotificationManager().vibrate(VibrationType.NOTIFICATION_ALARM).await();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    } catch (BandException e) {
                                        Toast.makeText(activity, "Could not connect to the band to send" +
                                                " vibration", Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                            .create().show();
                }
            });
            return false;
        } else

        {

		/* Set the options */
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap tileIcon = BitmapFactory.decodeResource(activity.getResources(), R.drawable.hamburger, options);

            BandTile tile = new BandTile.Builder(tileId, "Label Tile", tileIcon)
                    .setPageLayouts(createButtonLayout(), createTextLayout())
                    .build();
            if (client.getTileManager().addTile(activity, tile).await()) {
                return true;
            } else {
                Log.e(TAG, "Unable to add label tile to the band.\n");
                return false;
            }
        }
    }

    static boolean isEating = false;
    protected static void updatePages(BandClient client, UUID tileId) throws BandIOException {
        String activityText = (isEating) ? "Eating" : "Not eating";
        String buttonText = (isEating) ? "Stop eating" : "Start eating";
        Log.v(TAG, "isEating is " + isEating + " and button text is " + buttonText);

        client.getTileManager().setPages(tileId,
                new PageData(pageId1, 1)
                        .update(new TextBlockData(TXT_TITLE, activityText))
                        .update(new TextButtonData(BTN_EATING, buttonText))
                );
        isEating = !isEating;
    }


    private boolean doesTileExist(List<BandTile> tiles, UUID tileId) {
        for (BandTile tile : tiles) {
            if (tile.getTileId().equals(tileId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "tile event received!");
        if (intent.getAction() == TileEvent.ACTION_TILE_OPENED) {
        } else if (intent.getAction() == TileEvent.ACTION_TILE_CLOSED) {
        }

            /* ***** THIS IS THE ONLY EVENT WE ACTUALLY CARE ABOUT ***** */
        else if (intent.getAction() == TileEvent.ACTION_TILE_BUTTON_PRESSED) {
            TileButtonEvent buttonData = intent.getParcelableExtra(TileEvent.TILE_EVENT_DATA);
            Intent i = new Intent(context, TileManagerService.class);
            i.putExtra(TileManagerService.BUTTON_DATA_EXTRA, buttonData);
            context.startService(i);
        }
    }


    private class appTask extends AsyncTask<Void, Void, Void> {
        public appTask(BandInfo i, Context c, Activity a) {
            bandinfo = i;
            context = c;
            activity = a;
        }

        private BandInfo bandinfo;
        private Context context;
        private Activity activity;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                BandClient client = getConnectedBandClient(bandinfo, context);
                if (client != null) {
                    Log.v(TAG, "Band is connected.\n");
                    if (addTile(bandinfo, activity)) {
                        UUID key = generateUUID(bandinfo.getMacAddress());
                        updatePages(client, key);
                    }
                } else {
                    Log.e(TAG, "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
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
                e.printStackTrace();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private final String ID_BASE = "cc0D508F-70A3-47D4-BBA3-";

    private UUID generateUUID(String mac) {
        mac = mac.replace(":", "");
        Log.v(TAG, "Generated uuid with " + ID_BASE + mac);
        return UUID.fromString(ID_BASE + mac);
    }

    protected static BandInfo infoFromUUID(UUID uuid) {
        String mac = uuid.toString().substring(24);
        mac = mac.toUpperCase();
        Log.v(TAG, "Mac without colons: " + mac);
        mac = mac.substring(0, 2) + ":"
                + mac.substring(2, 4) + ":"
                + mac.substring(4, 6) + ":"
                + mac.substring(6, 8) + ":"
                + mac.substring(8, 10) + ":"
                + mac.substring(10, 12);
        Log.v(TAG, "mac with colons: " + mac);


        BandInfo[] bands = BandClientManager.getInstance().getPairedBands();
        for (BandInfo band : bands) {
            if (band.getMacAddress().equals(mac)) {
                return band;
            } else {
                Log.v(TAG, band.getMacAddress() + " does not equal " + mac);
            }
        }
        Log.e(TAG, "No info found");
        return null;
    }

    /* *************************** TILE LAYOUT CREATORS *************************** */
    private static final int BTN_EATING = 31;
    private static final int BTN_DRINKING = 32;
    private static final int BTN_NOTHING = 33;
    private static final int TXT_TITLE = 21;
    private static final int TXT_ACTIVITY = 22;

    private PageLayout createTextLayout() {
        return new PageLayout(
                new ScrollFlowPanel(15, 0, 260, 125, FlowPanelOrientation.VERTICAL)
                        .addElements(new TextBlock(0, 0, 260, 45, TextBlockFont.MEDIUM).setMargins(0, 5, 0, 0)
                                .setId(TXT_TITLE).setAutoWidthEnabled(true))
//                        .addElements(new TextBlock(0, 0, 260, 90, TextBlockFont.SMALL).setMargins(0, 5, 0, 0)
//                                .setId(TXT_ACTIVITY).setAutoWidthEnabled(true))
                        .addElements(new TextButton(0, 0, 260, 2 * 45).setMargins(0, 5, 0, 0)
                            .setId(BTN_EATING).setPressedColor(Color.BLUE)));
    }

    private PageLayout createButtonLayout() {
        return new PageLayout(
                new ScrollFlowPanel(15, 0, 260, 125, FlowPanelOrientation.VERTICAL));
//                        .addElements(new TextButton(0, 0, 260, 2 * 45).setMargins(0, 5, 0, 0)
//                                .setId(BTN_EATING).setPressedColor(Color.BLUE)));
//                        .addElements(new TextButton(0, 0, 260, 2 * 45).setMargins(0, 5, 0, 0)
//                                .setId(BTN_DRINKING).setPressedColor(Color.BLUE))
//                        .addElements(new TextButton(0, 0, 260, 2 * 45).setMargins(0, 5, 0, 0)
//                                .setId(BTN_NOTHING).setPressedColor(Color.BLUE)));
    }

    /* *************************** GENERIC BAND STUFF *************************** */
    protected static BandClient getConnectedBandClient(BandInfo info, Context context) throws
            InterruptedException, BandException {
        BandClient client;
        client = BandClientManager.getInstance().create(context, info);
        if (ConnectionState.CONNECTED == client.connect().await()) {
            return client;
        } else {
            throw new BandException("Could not connect to client", BandErrorType.UNKNOWN_ERROR);
        }
    }
}
