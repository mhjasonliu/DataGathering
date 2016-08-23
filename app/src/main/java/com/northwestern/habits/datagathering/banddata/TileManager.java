package com.northwestern.habits.datagathering.banddata;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandErrorType;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
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

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by William on 8/23/2016
 */
public class TileManager {
    private String activity = "Nothing";
    private final String TAG = "TileManager";
    /** Maps tile to a band */
    private HashMap<UUID, BandInfo> bands = new HashMap<>();

    public TileManager(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TileEvent.ACTION_TILE_OPENED);
        filter.addAction(TileEvent.ACTION_TILE_BUTTON_PRESSED);
        filter.addAction(TileEvent.ACTION_TILE_CLOSED);
        context.registerReceiver(messageReceiver, filter);
    }

    public void sendTile(Activity a, String macAddress) {
        BandInfo[] bands = BandClientManager.getInstance().getPairedBands();
        for (BandInfo b :
                bands) {
            if (b.getMacAddress().equals(macAddress)) {
                new appTask(b,a,a).execute();
                break;
            }
        }

    }
    public void close(Context context) {
        context.unregisterReceiver(messageReceiver);
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
                    UUID tid = buttonData.getTileID();
                    if (onButtonClicked(buttonData.getElementID()) &&
                            bands.containsKey(tid)) {
                        updatePages(getConnectedBandClient(bands.get(tid), context), tid);
                    }
                } catch (InterruptedException | BandException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private boolean onButtonClicked(int clickedID) throws BandIOException {
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
        return !Objects.equals(activity, prev);
    }


    /* *************************** TILE MANAGING FUNCTIONS *************************** */
//    private static final UUID tileId = UUID.fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
    private static final UUID pageId1 = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd00");
    private static final UUID pageId2 = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd01");

    private boolean addTile(BandInfo info, Activity activity) throws Exception {
        BandClient client = getConnectedBandClient(info, activity);
        UUID tileId = null;
        if (bands.containsValue(info)) {
            for (UUID key :
                    bands.keySet()) {
                if (bands.get(key) == info) {
                    tileId = key;
                }
            }

            if (tileId == null) {
                tileId = UUID.fromString(info.getMacAddress());
                bands.put(tileId, info);
            }
        } else {
            tileId = UUID.randomUUID();
            bands.put(tileId, info);
        }

        if (doesTileExist(client.getTileManager().getTiles().await(), tileId)) {
            client.getTileManager().removeTile(tileId);
        }

		/* Set the options */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap tileIcon = BitmapFactory.decodeResource(activity.getResources(), R.drawable.hamburger, options);

        BandTile tile = new BandTile.Builder(tileId, "Button Tile", tileIcon)
                .setPageLayouts(createButtonLayout(), createTextLayout())
                .build();
        if (client.getTileManager().addTile(activity, tile).await()) {
            return true;
        } else {
            Log.e(TAG, "Unable to add button tile to the band.\n");
            return false;
        }
    }

    private void updatePages(BandClient client, UUID tileId) throws BandIOException {
        client.getTileManager().setPages(tileId,
                new PageData(pageId2, 0)
                        .update(new TextButtonData(BTN_EATING, "Eating"))
                        .update(new TextButtonData(BTN_DRINKING, "Drinking"))
                        .update(new TextButtonData(BTN_NOTHING, "Note to self")),
                new PageData(pageId1, 1)
                        .update(new TextBlockData(TXT_TITLE, "Current Activity"))
                        .update(new TextBlockData(TXT_ACTIVITY, activity)));
    }


    private boolean doesTileExist(List<BandTile> tiles, UUID tileId) {
        for (BandTile tile : tiles) {
            if (tile.getTileId().equals(tileId)) {
                return true;
            }
        }
        return false;
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
                        for (UUID key :
                                bands.keySet()) {
                            if (bands.get(key) == bandinfo) {
                                updatePages(client, key);
                                break;
                            }
                        }
                    }
                } else {
                    Log.e(TAG,"Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
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
                Log.e(TAG,exceptionMessage);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
    }

    /* *************************** TILE LAYOUT CREATORS *************************** */
    private final int BTN_EATING = 31;
    private final int BTN_DRINKING = 32;
    private final int BTN_NOTHING = 33;
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

    private PageLayout createButtonLayout() {
        return new PageLayout(
                new ScrollFlowPanel(15, 0, 260, 125, FlowPanelOrientation.VERTICAL)
                        .addElements(new TextButton(0, 0, 260, 45).setMargins(0, 5, 0, 0)
                                .setId(BTN_EATING).setPressedColor(Color.BLUE))
                        .addElements(new TextButton(0, 0, 260, 45).setMargins(0, 5, 0, 0)
                                .setId(BTN_DRINKING).setPressedColor(Color.BLUE))
                        .addElements(new TextButton(0, 0, 260, 45).setMargins(0, 5, 0, 0)
                                .setId(BTN_NOTHING).setPressedColor(Color.BLUE)));
    }

    /* *************************** GENERIC BAND STUFF *************************** */

    private BandClient getConnectedBandClient(BandInfo info, Context context) throws InterruptedException, BandException {
        BandClient client = null;
        client = BandClientManager.getInstance().create(context, info);
        if (ConnectionState.CONNECTED == client.connect().await()) {
            return client;
        }
        else {
            throw new BandException("Could not connect to client", BandErrorType.UNKNOWN_ERROR);
        }
    }
}
