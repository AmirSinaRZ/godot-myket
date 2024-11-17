package com.example.godotmyket;

import android.util.Log;

import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ir.myket.billingclient.IabHelper;
import ir.myket.billingclient.util.IabResult;
import ir.myket.billingclient.util.Inventory;
import ir.myket.billingclient.util.Purchase;
import ir.myket.billingclient.util.SkuDetails;

public class GodotMyket extends GodotPlugin {
    private IabHelper mHelper;
    private String TAG = "godot";

    /**
     * Base constructor passing a {@link Godot} instance through which the plugin can access Godot's
     * APIs and lifecycle events.
     *
     * @param godot
     */
    public GodotMyket(Godot godot) {
        super(godot);
    }

    @Override
    public String getPluginName() {
        return "GodotMyket";
    }

    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new HashSet<>();
        signals.add(new SignalInfo("connection_succeed"));
        signals.add(new SignalInfo("connection_failed", String.class));
        signals.add(new SignalInfo("query_inventory_finished", Boolean.class, String.class, Dictionary.class));
        signals.add(new SignalInfo("query_inventory_failed", String.class));
        return signals;
    }

    @UsedByGodot
    public void connect_to_myket(String public_key) {
        mHelper = new IabHelper(getActivity(), public_key);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (mHelper == null) return;
                if (result.isSuccess()) {
                    emitSignal("connection_succeed");
                } else if (result.isFailure()) {
                    emitSignal("connection_failed", result.getMessage());
                }
            }
        });
    }

    @UsedByGodot
    public void query_inventory_async(boolean query_sku_details, String[] item_skus) {
        try {
            getGodot().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<String> skus = Arrays.asList(item_skus);
                    mHelper.queryInventoryAsync(query_sku_details, skus, new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            if (mHelper == null) return;
                            Dictionary inventory = new Dictionary();
                            Dictionary products = new Dictionary();
                            Dictionary purchases = new Dictionary();
                            for (SkuDetails item : inv.getAllProducts()) {
                                Dictionary product = new Dictionary();
                                product.put("sku", item.getSku());
                                product.put("type", item.getType());
                                product.put("price", item.getPrice());
                                product.put("title", item.getTitle());
                                product.put("description", item.getDescription());
                                products.put(item.getSku(), product);
                            }
                            for (Purchase item : inv.getAllPurchases()) {
                                Dictionary purchase = new Dictionary();
                                purchase.put("item_type", item.getItemType());
                                purchase.put("order_id", item.getOrderId());
                                purchase.put("sku", item.getSku());
                                purchase.put("purchase_time", item.getPurchaseTime());
                                purchase.put("purchase_state", item.getPurchaseState());
                                purchase.put("developer_payload", item.getDeveloperPayload());
                                purchase.put("token", item.getToken());
                                purchase.put("original_json", item.getOriginalJson());
                                purchase.put("package_name", item.getPackageName());
                                purchase.put("signature", item.getSignature());
                                purchases.put(item.getOrderId(), purchase);
                            }
                            inventory.put("products", products);
                            inventory.put("purchases", purchases);
                            emitSignal("query_inventory_finished", result.isSuccess(), result.getMessage(), inventory);
                        }
                    });
                }
            });

        } catch (Exception e) {
            emitSignal("query_inventory_failed", "Error querying inventory. Another async operation in progress.");
        }
    }
}
