package wesleycoelho.cursoudemy.arcgisapi;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class Permissao {

    public static boolean validaPermissoes(String[] permissoes, Activity activity, int requestCode){

        //verificar se o usuário está utilizando a versão maio que a Marshmelow 23
        if(Build.VERSION.SDK_INT >= 23){
            List<String> listaPermissoes = new ArrayList<>();

            /*Percorre as permissoes verificando
            uma a uma se já tem permissão liberada
             */
            for (String permissao: permissoes){
                //checa se a permissao foi concedida
                boolean temPermissao = ContextCompat.checkSelfPermission(activity, permissao) == PackageManager.PERMISSION_GRANTED;
                if(!temPermissao){
                    listaPermissoes.add(permissao);
                }
                /*caso a lista esteja vazia não necessário mais solicitar permissao*/
                if( listaPermissoes.isEmpty() ) return true;
                String[] novasPermissoes = new String[ listaPermissoes.size()];
                listaPermissoes.toArray(novasPermissoes);
                //Solicita permissao
                ActivityCompat.requestPermissions(activity, novasPermissoes, requestCode);
            }
        }
        return true;
    }
}
