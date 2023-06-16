package cn.ruc.readio.ui.userpage.login;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;

import cn.ruc.readio.R;
import cn.ruc.readio.util.Auth;
import cn.ruc.readio.util.Tools;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RegisterFormFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RegisterFormFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RegisterFormFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RegisterFormFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RegisterFormFragment newInstance(String param1, String param2) {
        RegisterFormFragment fragment = new RegisterFormFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_register_form, container, false);
        TextView btn = view.findViewById(R.id.registerBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    onClickRegisterBtn();
                } catch (JSONException e) {
                    if(getActivity()!=null) {
                        Tools.my_toast(getActivity(), "啊哦！出错啦");
                    }
                }
            }
        });
        return view;
    }

    private void onClickRegisterBtn() throws JSONException {
        EditText phoneNumberEditTextReg = (EditText) getView().findViewById(R.id.phoneNumberEditTextReg);
        EditText passwordEditTextReg = (EditText) getView().findViewById(R.id.passwordEditTextReg);
        EditText passwordRepeatEditTextReg = (EditText) getView().findViewById(R.id.passwordRepeatEditTextReg);
        String phoneNumber = phoneNumberEditTextReg.getText().toString();
        String password1 = passwordEditTextReg.getText().toString();
        String password2 = passwordRepeatEditTextReg.getText().toString();
        if(!password1.equals(password2)){
            if(getActivity()!=null) {
                Tools.my_toast(getActivity(), "两次密码不一致");
            }
            return;
        }
        Auth auth = new Auth();
        if(getActivity()!=null) {
            auth.register((LoginActivity) getActivity(), phoneNumber, password1);
        }
    }
}