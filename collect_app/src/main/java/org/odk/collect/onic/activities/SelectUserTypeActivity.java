package org.odk.collect.onic.activities;

import android.content.Intent;
//import android.support.annotation.NonNull;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;

import java.util.Objects;

public class SelectUserTypeActivity extends AppCompatActivity {

    private Button institucional_user_button;

    private Button particular_user_button;

    private Button button_toback;

    private static final String TAG = "ONIC" ;
    private FirebaseAuth mAuth; //conexión a la base de datos de firebase

    //Escucha si los datos son correctos para manejar el evento que lleva a la siguiente actividad
    private FirebaseAuth.AuthStateListener mAuthListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_user_type);


        mAuth = FirebaseAuth.getInstance();




        //Institucional User button. CreadoJorge
        institucional_user_button = (Button) findViewById(R.id.institucional_user_button);
        institucional_user_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                Intent i = new Intent(getApplicationContext(),
                        LoginActivity.class); // Call to terms and Conditions Activity
                startActivity(i);
            }
        });

        //Particular User button. CreadoJorge
        particular_user_button = (Button) findViewById(R.id.particular_user_button);
        particular_user_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                LoginUser();
                /*Intent i = new Intent(getApplicationContext(),
                        MainMenuActivity.class); // Call to terms and Conditions Activity
                startActivity(i);*/
            }
        });

        //Accept Termns button. CreadoJorge
        button_toback = (Button) findViewById(R.id.to_backButton);
        button_toback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                Intent i = new Intent(getApplicationContext(),
                        AcceptTermsAndConditionsActivity.class); // Call to terms and Conditions Activity
                startActivity(i);
            }
        });

    }

    //Selecciona Usuario Particular
    private void LoginUser(){

        FirebaseUser currentUser = mAuth.getCurrentUser();

        String email = "usuarioparticular@gmail.com";
        String password = "usuarioparticular2020smtonic";

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                       if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            /*Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(SelectUserTypeActivity.this,MainMenuActivity.class));
                            //updateUI(user);*/

                           Log.d(TAG, "signInWithEmail:success");

                           //build the intent with kobo project id to be send to MainMenuActivity
                           Intent id_odk_project_intent =  new Intent(SelectUserTypeActivity.this,MainMenuActivity.class);

                           Bundle id_ins_module_bundle = new Bundle();

                           //llave idProjectodk con id de formulario de sintomas
                           id_ins_module_bundle.putString("idProjectodk","sintomas"); //kobo Kuery
                           //id_ins_module_bundle.putString("idProjectodk","aSZFjc6cT6Jhzb5y2dk9nQ"); //onic agregate sintomas

                           //llave opcionmodulo para conexion con servidor que tiene formulario sintomas
                           id_ins_module_bundle.putInt("opcionmodulo",1);

                           //agregate ONIC
                           //monitoreoterritorial
                           //Kobo id to particular User
                           //id_ins_module_bundle.putString("idProjectodk","afwrcrKsVqpcwq64Eq24en"); //ONIC: sintomas_01 //prueba kobo afwrcrKsVqpcwq64Eq24en


                           id_odk_project_intent.putExtras(id_ins_module_bundle); //asign project id to intent
                           // Log.e("EN SELECT MODULE NEW: ", id_ins_module_bundle.getString("idProjectODK"));
                           startActivity(id_odk_project_intent);//call MainMenuActivity with kobo prueba id afwrcrKsVqpcwq64Eq24en
                       /*
                       if(true)
                        {

                           //build the intent with kobo project id to be send to MainMenuActivity
                           Intent id_kobo_project_intent =  new Intent(SelectUserTypeActivity.this,MainMenuActivity.class);

                           Bundle id_ins_module_bundle = new Bundle();

                           //Kobo id to particular User
                           id_ins_module_bundle.putString("idProjectODK","atdQRbHwKxwbuAy7Uzdf8V"); //prueba snit
                           id_kobo_project_intent.putExtras(id_ins_module_bundle); //asign kobo project id to intent
                          // Log.e("EN SELECT MODULE NEW: ", id_ins_module_bundle.getString("idProjectODK"));
                           startActivity(id_kobo_project_intent);//call MainMenuActivity with kobo id
                        */

                       } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(SelectUserTypeActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            //updateUI(null);
                        }

                        // ...
                    }
                });
    }
}