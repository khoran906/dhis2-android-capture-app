package org.dhis2.usescases.datasets.dataSetTable;

import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.dataset.DataSet;

import java.util.List;

import io.reactivex.Flowable;

public interface DataSetTableRepository {
    Flowable<DataSet> getDataSet();

    Flowable<List<String>> getSections();

    Flowable<State> dataSetStatus();

    Flowable<String> getCatComboName(String catcomboUid);

}
