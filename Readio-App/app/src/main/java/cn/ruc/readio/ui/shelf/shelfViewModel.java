package cn.ruc.readio.ui.shelf;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class shelfViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public shelfViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}