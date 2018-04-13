package com.dhis2.data.forms.dataentry.fields.age;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.dhis2.R;
import com.dhis2.data.forms.dataentry.fields.Row;
import com.dhis2.data.forms.dataentry.fields.RowAction;
import com.dhis2.databinding.FormAgeCustomBinding;

import io.reactivex.processors.FlowableProcessor;

/**
 * Created by frodriguez on 20/03/2018.
 */

public class AgeRow implements Row<AgeHolder, AgeViewModel> {

    private final LayoutInflater inflater;
    private final boolean isBgTransparent;
    private final FlowableProcessor<RowAction> processor;

    public AgeRow(LayoutInflater layoutInflater, FlowableProcessor<RowAction> processor, boolean isBgTransparent) {
        this.inflater = layoutInflater;
        this.isBgTransparent = isBgTransparent;
        this.processor = processor;
    }

    @NonNull
    @Override
    public AgeHolder onCreate(@NonNull ViewGroup parent) {
        FormAgeCustomBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.form_age_custom, parent, false);
        binding.customAgeview.setIsBgTransparent(isBgTransparent);
        return new AgeHolder(binding, processor);
    }

    @Override
    public void onBind(@NonNull AgeHolder viewHolder, @NonNull AgeViewModel viewModel) {
        viewHolder.update(viewModel);
    }
}
