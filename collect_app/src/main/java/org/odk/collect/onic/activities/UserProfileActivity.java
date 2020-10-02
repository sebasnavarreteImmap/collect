package org.odk.collect.onic.activities;

import android.content.Intent;
import android.net.Uri;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.odk.collect.onic.R;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private TextView userName;
    private Button logoutButton;
    private Button button_toback;

   // FirebaseFirestore db = FirebaseFirestore.getInstance();

    //private static final String TAG = "ONIC" ;

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile_layout);

        userName = (TextView) findViewById(R.id.userName);

        userName.setText("USUARIO PRUEBA");




        if (user != null) {
            Log.e("USUARIO NO ES NULL",user.toString());
            // Name, email address, and profile photo Url
            //String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            Log.e("EMAIL DEL USER: ",email.toString());

            userName.setText(email);

            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getIdToken() instead.
            String uid = user.getUid();
            Log.e("USER UID: ", uid.toString());
/*
            db.collection("users").document(email).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        String todoDocument = document.getId();
                        Object data = document.getData();
                        Log.e("ID DOCUMENT: ", todoDocument);
                        Log.e("EMAIL DOCUMENT: ", data.toString());
                        Log.e("ESTE ES EL DOCUMENT: ",document.toString());

                        //userName.setText();
                    }else{
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }

                }
            });*/

                /*
            db.collection("users")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                    String documento = document.getId();
                                    Log.e("DOCUMENTO ES: ", documento);
                                    Map<String, Object> data = document.getData();
                                    Log.e("LA DATA DE FIRESTORE: ",data.toString());
                                }
                            } else {
                                Log.w(TAG, "Error getting documents.", task.getException());
                            }
                        }
                    });*/
        }

        logoutButton = (Button) findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {


                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(UserProfileActivity.this, SelectUserTypeActivity.class);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//makesure user cant go back
                startActivity(intent);
            }



        });



        button_toback = (Button) findViewById(R.id.to_backButton);

        button_toback.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){

                // FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(UserProfileActivity.this,MainMenuActivity.class));


            }

        });


    }
}