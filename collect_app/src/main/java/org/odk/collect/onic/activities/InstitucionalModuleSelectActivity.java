package org.odk.collect.onic.activities;

import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.auth0.android.Auth0;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.odk.collect.onic.widgets.StringWidget;


public class InstitucionalModuleSelectActivity extends AppCompatActivity {

    private Button to_backButton;

    //Firebase
    private FirebaseAuth mAuth;

    //auth0
    //private Auth0 auth0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.institucional_module_select);

        //To BACK button. CreadoJorge
        to_backButton = (Button) findViewById(R.id.to_backButton);
        to_backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                Intent i = new Intent(getApplicationContext(),
                        SelectUserTypeActivity.class); // Call to terms and Conditions Activity
                startActivity(i);
            }
        });

        //firebase
        mAuth = FirebaseAuth.getInstance();

        // auth0 = new Auth0(this);

        //auth0.setOIDCConformant(true);
    }

    //Select Institucional Module. Send identificator of Kobo form  respective  of module selected to MainActivity
    public void select_module(View view){

        //variable to asign module name selected
        String select_moduleOp = "";

        //validate the module selected button (image or button) and asigned the name to variable
        if(view.getId() == R.id.vigilancia_comunitaria_button1 || view.getId() == R.id.vigilancia_comunitaria_button2 ){
            select_moduleOp =  "vigilancia_comunitaria";
        }else if(view.getId() == R.id.derechos_humanos_button1 || view.getId() == R.id.derechos_humanos_button2 ){
            select_moduleOp =  "derechos_humanos";
        }else if(view.getId() == R.id.territorios_button1 || view.getId() == R.id.territorios_button2 ){
            select_moduleOp =  "territorios";
        }else if(view.getId() == R.id.movilizacion_social_button1 || view.getId() == R.id.movilizacion_social_button2 ){
            select_moduleOp =  "movilizacion_social";
        }else if(view.getId() == R.id.economias_propias_button1 || view.getId() == R.id.economias_propias_button2 ){
            select_moduleOp =  "economias_propias";
        }else if(view.getId() == R.id.ambiental_button1 || view.getId() == R.id.ambiental_button2 ){
            select_moduleOp =  "ambiental";
        }

        //build the intent with kobo project id to be send to MainMenuActivity
        Intent id_odk_project_intent =  new Intent(InstitucionalModuleSelectActivity.this,MainMenuActivity.class);

        Bundle id_ins_module_bundle = new Bundle();

        //Bundle para opcionmodulo
        Bundle opcion_modulo = new Bundle();


        //select the case according with model name, asign ddk project id to intent and call MainMenuActivity
        //switch (view.getId()){
        switch (select_moduleOp){
            //case R.id.vigilancia_comunitaria_button1:
            case "vigilancia_comunitaria":

                String formularioSintomas = Collect.getInstance().getString(R.string.idFormularioSintomas);
                id_ins_module_bundle.putString("idProjectodk",formularioSintomas); //anterior:aSZFjc6cT6Jhzb5y2dk9nQ a5NzyoHqgaSsSRhqmb2J6M  pandemia: aSZFjc6cT6Jhzb5y2dk9nQ  odkaggregate sintomas_01 onic aggregate project id
                //aSZFjc6cT6Jhzb5y2dk9nQ es para kobo
                //Log.e("EN SELECT MODULE : ", id_ins_module_bundle.getString("idProjectodk"));
                opcion_modulo.putInt("opcionmodulo",1);

                break;

            case "derechos_humanos":

                String formularioDerechosHumanos = Collect.getInstance().getString(R.string.idFormularioDerechosHumanos);

                id_ins_module_bundle.putString("idProjectodk",formularioDerechosHumanos);
                //Log.e("EN SELECT MODULE : ", id_ins_module_bundle.getString("idProjectodk"));
                opcion_modulo.putInt("opcionmodulo",2);

                break;

            case "territorios":

                String formularioTerritorios = Collect.getInstance().getString(R.string.idFormularioTerritorios);

                id_ins_module_bundle.putString("idProjectodk",formularioTerritorios);
                opcion_modulo.putInt("opcionmodulo",3);
                //Log.e("EN SELECT MODULE : ", id_ins_module_bundle.getString("idProjectodk"));
                break;

            case "movilizacion_social":

                String formularioMovilizacionSocial = Collect.getInstance().getString(R.string.idFormularioMovilizacionSocial);

                id_ins_module_bundle.putString("idProjectodk",formularioMovilizacionSocial);
                opcion_modulo.putInt("opcionmodulo",4);
                //Log.e("EN SELECT MODULE : ", id_ins_module_bundle.getString("idProjectodk"));
                break;

            case "economias_propias":

                String formularioEconomiasPropias = Collect.getInstance().getString(R.string.idFormularioEconomiasPropias);

                id_ins_module_bundle.putString("idProjectodk",formularioEconomiasPropias);
                opcion_modulo.putInt("opcionmodulo",5);
                //Log.e("EN SELECT MODULE : ", id_ins_module_bundle.getString("idProjectodk"));
                break;

            case "ambiental":

                String formularioAmbiental = Collect.getInstance().getString(R.string.idFormularioAmbiental);

                id_ins_module_bundle.putString("idProjectodk",formularioAmbiental); //anterior:afwrcrKsVqpcwq64Eq24en aWzTWZ4Yn8hnLPosiEZujs prueba snit //smt a5NzyoHqgaSsSRhqmb2J6M
                //Log.e("EN SELECT MODULE : ", id_ins_module_bundle.getString("idProjectodk"));
                opcion_modulo.putInt("opcionmodulo",6);

                break;

            default:
                break;
        }

        id_odk_project_intent.putExtras(id_ins_module_bundle); //asign kobo project id to intent
        //Log.e("EN SELECT MODULE NEW: ", id_ins_module_bundle.getString("idProjectodk"));

        //valor de la opcion para conexion a servidor de todos los formularios:
        id_odk_project_intent.putExtras(opcion_modulo);
        startActivity(id_odk_project_intent);//call MainMenuActivity*/


    }

    public void onStart(){
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        //updateUI(currentUser);
        //Log.e("CURRENT USER",currentUser.toString());
    }


}