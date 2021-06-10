package org.odk.collect.onic.activities;

import android.content.Intent;
import android.os.Bundle;
//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;

public class AcceptTermsAndConditionsActivity extends AppCompatActivity {

    private Button acceptTermsButton;
    private TextView goto_termns_link;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accept_terms_and_conditions);

        acceptTermsButton = (Button) findViewById(R.id.accept_termns_button);
        acceptTermsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                Intent i = new Intent(getApplicationContext(),
                        SelectUserTypeActivity.class);
                startActivity(i);
            }
        });

        goto_termns_link = (TextView) findViewById(R.id.goto_termns_link);
        goto_termns_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                Intent i = new Intent(getApplicationContext(),
                        TermsAndConditionsActivity.class);
                startActivity(i);
            }
        });



    }
}