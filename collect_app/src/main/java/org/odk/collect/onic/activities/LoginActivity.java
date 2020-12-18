package org.odk.collect.onic.activities;

import android.app.Dialog;
import android.content.Intent;
//import android.support.annotation.NonNull;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONObject;
import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.json.Json;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "ONIC" ;
    private EditText emailField, passwordField;
    private Button loginButton;
    private Button button_toback;

    private FirebaseAuth mAuth; //conexión a la base de datos de firebase

    //Escucha si los datos son correctos para manejar el evento que lleva a la siguiente actividad
    private FirebaseAuth.AuthStateListener mAuthListener;


    //auth0
    private Auth0 auth0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        emailField = (EditText) findViewById(R.id.email);
        passwordField = (EditText) findViewById(R.id.password);

        mAuth = FirebaseAuth.getInstance();



        loginButton = (Button) findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){



                    if(!TextUtils.isEmpty(emailField.getText())) {

                        if(!TextUtils.isEmpty(passwordField.getText())){
                            LoginUser();
                        }else{
                            Toast.makeText(LoginActivity.this, "Debe ingresar email y password!",
                                    Toast.LENGTH_SHORT).show();

                        }


                    } else {
                        Toast.makeText(LoginActivity.this, "Debe ingresar email y password!",
                                Toast.LENGTH_SHORT).show();
                    }



                }

        });

        button_toback = (Button) findViewById(R.id.to_backButton);
        button_toback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                Intent i = new Intent(getApplicationContext(),
                        SelectUserTypeActivity.class); // Call to terms and Conditions Activity
                startActivity(i);
            }
        });

        //OAUTH0
        //Auth0 account = new Auth0("{YOUR_CLIENT_ID}", "{YOUR_DOMAIN}");
        /*Auth0 account = new Auth0("{EQHewJdZHzzX21KVuMDzFq0lrAjffZfx}", "{dev-smtponic.us.auth0.com}");

        account.setOIDCConformant(true);*/

        //onResume();




    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUser currentUser = mAuth.getCurrentUser();


        if (currentUser == null) {


            Toast.makeText(LoginActivity.this, "No se detecta ningún Usuario. Ingrese con sus datos",
                    Toast.LENGTH_SHORT).show();
        } else {

            //String uid = currentUser.getUid();
            //String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            if (Objects.equals(email, "usuarioparticular@gmail.com")) {

                FirebaseAuth.getInstance().signOut();

                Toast.makeText(LoginActivity.this, "Cambio a Usuario Institucional. Ingrese con sus datos.",
                        Toast.LENGTH_LONG).show();



            } else  {
                //Log.e("LO QUE HAY EN UID", uid );
                //Log.e("LO QUE HAY EN NAME", name);
                Log.e("LO QUE HAY EN EMAIL", email);

                Log.e("NO ES NULO", "NO ES NULO VA A INSTITUCIONAL");

                startActivity(new Intent(LoginActivity.this, InstitucionalModuleSelectActivity.class));


            }


        }

    }

    private void LoginUser(){
        //Log.e("EMAILFIELD: ", emailField.toString());
        //Log.e( "PASSW: ", passwordField.getText().toString());


            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();

            // Firebase:
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // Log.e("RESULTADO DE TASK",task.toString());
                            if (task.isSuccessful()) {

                                Log.e("SI ENTRE EN ISSUCCESFUL","ENTREEEE");
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                startActivity(new Intent(LoginActivity.this,InstitucionalModuleSelectActivity.class));


                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                Log.e("NADA NO ENTRE","NO ENTRE");
                                //updateUI(null);
                            }

                            // ...
                        }
                    });







    }

    /*private void updateUI(FirebaseUser user) {
    }*/

    @Override
    public void onStart() {

        super.onStart();

        /*

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();


        if(currentUser == null){





            Toast.makeText(LoginActivity.this, "Ingrese con sus datos.",
                    Toast.LENGTH_SHORT).show();
        }else{

            Log.e("LO QUE HAY EN UID", currentUser.toString() );

            //String uid = currentUser.getUid();
            //String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            if(email == "usuarioparticular@gmail.com"){

                FirebaseAuth.getInstance().signOut();

                Toast.makeText(LoginActivity.this, "Ingrese con sus datos!.",
                        Toast.LENGTH_SHORT).show();

            }else if(email != "usuarioparticular@gmail.com"){
                //Log.e("LO QUE HAY EN UID", uid );
                //Log.e("LO QUE HAY EN NAME", name);
                Log.e("LO QUE HAY EN EMAIL", email);

                Log.e("NO ES NULO","NO ES NULO VA A INSTITUCIONAL");

                startActivity(new Intent(LoginActivity.this,InstitucionalModuleSelectActivity.class));


            }





        }

        */
    }

}