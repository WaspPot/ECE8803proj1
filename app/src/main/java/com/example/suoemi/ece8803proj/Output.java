package com.example.suoemi.ece8803proj;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;
import com.joptimizer.optimizers.OptimizationResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Created by Suoemi on 3/30/2017.
 */

public class Output extends AppCompatActivity {

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private int c1;
    private int u1;
    private int returnCode;
    public double[] sol;
    private double[] c;
    private double[] B;
    private double[] ub;
    private Map<String, Object> usr_sell;
    private Map<String, Object> usr_buy;
    public double outp;
    public double outa;

    private FirebaseAuth mAuth;
    private FirebaseUser muser;
    private DatabaseReference databaseref;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final String TAG = "BuyerInput";

    TableRow tr;
    TextView sellusr;
    TextView sellamt;
    TextView buytot;
    TextView pricetot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ouput);

        databaseref = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        RelativeLayout relLay = (RelativeLayout) findViewById(R.id.info);
        TableLayout tabLay = (TableLayout) findViewById(R.id.tableinfo);

        sellusr = new TextView(this);
        sellamt = new TextView(this);
        tr = new TableRow(this);

        tr.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(40, 0, 10, 10);

        sellusr.setText("Seller ID");
        //sellusr.setLayoutParams(params);
        sellusr.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sellusr.setTypeface(null, Typeface.BOLD);
        //sellusr.setPadding(5, 5, 5, 0);
        tr.addView(sellusr);

        sellamt.setText("Bidding Amount");
        //sellamt.setLayoutParams(params);
        sellamt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sellamt.setTypeface(null, Typeface.BOLD);
        //sellamt.setPadding(5, 5, 5, 0);
        tr.addView(sellamt);

        tabLay.addView(tr, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));

        try {
            CostMin();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(int i = 0; i < sol.length; i++)
        {
            tr = new TableRow(this);
            tr.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

            sellusr = new TextView(this);
            sellamt = new TextView(this);

            sellusr.setText("Seller" + i);
            sellusr.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tr.addView(sellusr);

            sellamt.setText(Double.toString(sol[i]));
            sellamt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tr.addView(sellamt);

            tabLay.addView(tr, new TableLayout.LayoutParams(
                    LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));
        }

        buytot.setText("EV driver total amount: " + outa);
        pricetot.setText("EV driver total price: " + outp);
        relLay.addView(buytot);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void CostMin() throws Exception {
        DbHandler db = new DbHandler(this);
        LPOptimizationRequest or = new LPOptimizationRequest();
        List<LoginData> loginDatasell = db.getAllSellLog();

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                usr_sell = (Map<String,Object>) dataSnapshot.getValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };

        databaseref.child("sellers").addValueEventListener(postListener);


        ValueEventListener postListener2 = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                usr_buy = (Map<String, Object>) dataSnapshot.getValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };

        databaseref.child("ev drivers").addValueEventListener(postListener2);

        ArrayList<Double> bidamt = new ArrayList<>();
        ArrayList<Double> bidpr = new ArrayList<>();
        ArrayList<Double> evreq = new ArrayList<>();

        for(Map.Entry<String, Object> entry : usr_sell.entrySet()){
            Map singlesell = (Map) entry.getValue();
            bidamt.add((Double) singlesell.get("bid amount"));
            bidpr.add((Double) singlesell.get("bid price"));
        }
        for(Map.Entry<String, Object> entry : usr_buy.entrySet()){
            Map singlebuy = (Map) entry.getValue();
            evreq.add((double) singlebuy.get("ev req"));
        }

        c = new double[bidpr.size()];
        for (int i = 0; i<c.length; i++)
        {
            c[i] = bidpr.get(i).doubleValue();
            System.out.print("Price" + c);
        }

        double[][] A = new double[][] {{1, 1, 1}};

        B = new double[evreq.size()];
        for (int i = 0; i<B.length; i++)
        {
            B[i] = evreq.get(i).doubleValue();
        }

        double[] lb = new double[] {0, 0, 0};

        ub = new double[bidamt.size()];
        for (int i = 0; i<ub.length; i++)
        {
            ub[i] = bidamt.get(i).doubleValue();
        }

        or.setC(c);
        or.setA(A);
        or.setB(B);
        or.setLb(lb);
        or.setUb(ub);
        or.setDumpProblem(true);

        //optimization
        LPPrimalDualMethod opt = new LPPrimalDualMethod();

        opt.setLPOptimizationRequest(or);
        try {
            returnCode = opt.optimize();
            assertEquals("success ", OptimizationResponse.SUCCESS, returnCode);
            this.sol = opt.getOptimizationResponse().getSolution();
            String log = "Solution: " + Arrays.toString(sol);
            Log.d("Solution:: ", log);
        }
        catch (Exception e)
        {
            fail(e.toString());
        }

        for(int i = 0; i < sol.length; i++)
        {
            outp += sol[i];
            outa += ub[i];
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Output Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}

