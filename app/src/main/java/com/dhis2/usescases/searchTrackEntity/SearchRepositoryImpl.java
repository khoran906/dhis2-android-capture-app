package com.dhis2.usescases.searchTrackEntity;

import android.content.ContentValues;
import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.dhis2.utils.CodeGenerator;
import com.squareup.sqlbrite2.BriteDatabase;

import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.enrollment.EnrollmentModel;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.option.OptionModel;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttributeModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValueModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.Observable;

/**
 * QUADRAM. Created by ppajuelo on 02/11/2017.
 */

public class SearchRepositoryImpl implements SearchRepository {

    private final BriteDatabase briteDatabase;

    private final String SELECT_PROGRAM_WITH_REGISTRATION = "SELECT * FROM " + ProgramModel.TABLE + " WHERE Program.programType='WITH_REGISTRATION' AND Program.trackedEntityType = ";
    private final String SELECT_PROGRAM_ATTRIBUTES = "SELECT TrackedEntityAttribute.* FROM " + TrackedEntityAttributeModel.TABLE +
            " INNER JOIN " + ProgramTrackedEntityAttributeModel.TABLE +
            " ON " + TrackedEntityAttributeModel.TABLE + "." + TrackedEntityAttributeModel.Columns.UID + " = " + ProgramTrackedEntityAttributeModel.TABLE + "." + ProgramTrackedEntityAttributeModel.Columns.TRACKED_ENTITY_ATTRIBUTE +
            " WHERE " + ProgramTrackedEntityAttributeModel.TABLE + "." + ProgramTrackedEntityAttributeModel.Columns.PROGRAM + " = ";
    private final String SELECT_ATTRIBUTES = "SELECT * FROM " + TrackedEntityAttributeModel.TABLE;
    private final String SELECT_OPTION_SET = "SELECT * FROM " + OptionModel.TABLE + " WHERE Option.optionSet = ";

    private final String GET_TRACKED_ENTITY_INSTANCES =
            "SELECT " +
                    TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.UID + ", " +
                    TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.CREATED_AT_CLIENT + ", " +
                    TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.LAST_UPDATED_AT_CLIENT + ", " +
                    TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.ORGANISATION_UNIT + ", " +
                    TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.TRACKED_ENTITY_TYPE + ", " +
                    TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.CREATED + ", " +
                    TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.LAST_UPDATED + ", " +
                    TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.STATE + ", " +
                    TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.ID + ", " +
                    EnrollmentModel.TABLE + "." + EnrollmentModel.Columns.TRACKED_ENTITY_INSTANCE + " AS enroll" + ", " +
                    TrackedEntityAttributeValueModel.TABLE + "." + TrackedEntityAttributeValueModel.Columns.TRACKED_ENTITY_INSTANCE + " AS attr" +
                    " FROM ((" + TrackedEntityInstanceModel.TABLE + " JOIN " + EnrollmentModel.TABLE + " ON enroll = " + TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.UID + ")" +
                    " JOIN " + TrackedEntityAttributeValueModel.TABLE + " ON attr = " + TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.UID + ")" +
                    " WHERE ";

    private final String SEARCH =
            "SELECT TrackedEntityInstance.*" +
                    " FROM ((" + TrackedEntityInstanceModel.TABLE + " JOIN " + EnrollmentModel.TABLE + " ON " + EnrollmentModel.TABLE + "." + EnrollmentModel.Columns.TRACKED_ENTITY_INSTANCE + " = " + TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.UID + ")" +
                    " JOIN (ATTR_QUERY) tabla ON tabla.trackedEntityInstance = TrackedEntityInstance.uid)" +
                    " WHERE ";


    private static final String[] TABLE_NAMES = new String[]{TrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.TABLE};
    private static final Set<String> TABLE_SET = new HashSet<>(Arrays.asList(TABLE_NAMES));
    private static final String[] TEI_TABLE_NAMES = new String[]{TrackedEntityInstanceModel.TABLE,
            EnrollmentModel.TABLE, TrackedEntityAttributeValueModel.TABLE};
    private static final Set<String> TEI_TABLE_SET = new HashSet<>(Arrays.asList(TEI_TABLE_NAMES));
    private final CodeGenerator codeGenerator;


    SearchRepositoryImpl(CodeGenerator codeGenerator, BriteDatabase briteDatabase) {
        this.codeGenerator = codeGenerator;
        this.briteDatabase = briteDatabase;
    }


    @NonNull
    @Override
    public Observable<List<TrackedEntityAttributeModel>> programAttributes(String programId) {
        return briteDatabase.createQuery(TABLE_SET, SELECT_PROGRAM_ATTRIBUTES + "'" + programId + "'")
                .mapToList(TrackedEntityAttributeModel::create);
    }

    @Override
    public Observable<List<TrackedEntityAttributeModel>> programAttributes() {
        return briteDatabase.createQuery(TrackedEntityAttributeModel.TABLE, SELECT_ATTRIBUTES)
                .mapToList(TrackedEntityAttributeModel::create);
    }

    @Override
    public Observable<List<OptionModel>> optionSet(String optionSetId) {
        return briteDatabase.createQuery(OptionModel.TABLE, SELECT_OPTION_SET + "'" + optionSetId + "'")
                .mapToList(OptionModel::create);
    }

    @Override
    public Observable<List<ProgramModel>> programsWithRegistration(String programTypeId) {
        return briteDatabase.createQuery(ProgramModel.TABLE, SELECT_PROGRAM_WITH_REGISTRATION + "'" + programTypeId + "'")
                .mapToList(ProgramModel::create);
    }

    @Override
    public Observable<List<TrackedEntityInstanceModel>> trackedEntityInstances(@NonNull String teType,
                                                                               @Nullable String programUid,
                                                                               @Nullable String enrollmentDate,
                                                                               @Nullable String incidentDate,
                                                                               @Nullable HashMap<String, String> queryData) {

        String teiTypeWHERE = "TrackedEntityInstance.trackedEntityType = '" + teType + "'";
        String TEI_FINAL_QUERY = GET_TRACKED_ENTITY_INSTANCES + teiTypeWHERE;
        if (programUid != null && !programUid.isEmpty()) {
            String programWHERE = "Enrollment.program = '" + programUid + "'";
            TEI_FINAL_QUERY += " AND " + programWHERE;
        }

        if (enrollmentDate != null && !enrollmentDate.isEmpty()) {
            String enrollmentDateWHERE = "Enrollment.enrollmentDate = '" + enrollmentDate + "'";
            TEI_FINAL_QUERY += " AND " + enrollmentDateWHERE;
        }
        if (incidentDate != null && !incidentDate.isEmpty()) {
            String incidentDateWHERE = "Enrollment.incidentData = '" + incidentDate + "'";
            TEI_FINAL_QUERY += " AND " + incidentDateWHERE;
        }

        if (queryData != null && !queryData.isEmpty()) {
            StringBuilder teiAttributeWHERE = new StringBuilder("");
            teiAttributeWHERE.append(TrackedEntityAttributeValueModel.TABLE + ".value IN (");
            for (int i = 0; i < queryData.keySet().size(); i++) {
                String dataValue = queryData.get(queryData.keySet().toArray()[i]);
                teiAttributeWHERE.append("'").append(dataValue).append("'");
                if (i < queryData.size() - 1)
                    teiAttributeWHERE.append(",");
            }
            teiAttributeWHERE.append(")");

//            TEI_FINAL_QUERY += " AND " + teiAttributeWHERE;
        }

        String valueFilter = "(" + TrackedEntityAttributeValueModel.TABLE + ".trackedEntityAttribute = 'ATTR_ID' AND " + TrackedEntityAttributeValueModel.TABLE + ".value LIKE '%ATTR_VALUE%')";
        if (queryData != null && !queryData.isEmpty()) {
            StringBuilder teiAttributeWHERE = new StringBuilder("");
            for (int i = 0; i < queryData.keySet().size(); i++) {
                String dataId = queryData.keySet().toArray()[i].toString();
                String dataValue = queryData.get(dataId);
                teiAttributeWHERE.append(valueFilter.replace("ATTR_ID", dataId).replace("ATTR_VALUE", dataValue));
                if (i < queryData.size() - 1)
                    teiAttributeWHERE.append(" AND ");
            }

            TEI_FINAL_QUERY += " AND " + teiAttributeWHERE;
        }

        String attrQuery = "(SELECT TrackedEntityAttributeValue.trackedEntityInstance FROM TrackedEntityAttributeValue WHERE " +
                "TrackedEntityAttributeValue.trackedEntityAttribute = 'ATTR_ID' AND TrackedEntityAttributeValue.value LIKE 'ATTR_VALUE%') t";
        StringBuilder attr = new StringBuilder("");
        for (int i = 0; i < queryData.keySet().size(); i++) {
            String dataId = queryData.keySet().toArray()[i].toString();
            String dataValue = queryData.get(dataId);

            if (i > 0)
                attr.append(" INNER JOIN  ");

            attr.append(attrQuery.replace("ATTR_ID", dataId).replace("ATTR_VALUE", dataValue));
            attr.append(i + 1);
            if (i > 0)
                attr.append(" ON t" + (i) + ".trackedEntityInstance = t" + (i + 1) + ".trackedEntityInstance ");
        }

        String search = SEARCH.replace("ATTR_QUERY", "SELECT t1.trackedEntityInstance FROM" + attr) + teiTypeWHERE;
        if (programUid != null && !programUid.isEmpty()) {
            String programWHERE = "Enrollment.program = '" + programUid + "'";
            search += " AND " + programWHERE;
        }
        search += " GROUP BY TrackedEntityInstance.uid";

        TEI_FINAL_QUERY += /*" AND " + EnrollmentModel.TABLE + "." + EnrollmentModel.Columns.ENROLLMENT_STATUS + " = '" + EnrollmentStatus.ACTIVE.name() + "'" +*/
                " GROUP BY " + TrackedEntityInstanceModel.TABLE + "." + TrackedEntityInstanceModel.Columns.UID;

        return briteDatabase.createQuery(TEI_TABLE_SET, search)
                .mapToList(TrackedEntityInstanceModel::create);
    }

    @NonNull
    @Override
    public Observable<String> saveToEnroll(@NonNull String teiType, @NonNull String orgUnit, @NonNull String programUid, @Nullable String teiUid, HashMap<String, String> queryData) {
        Date currentDate = Calendar.getInstance().getTime();
        return Observable.defer(() -> {
            TrackedEntityInstanceModel trackedEntityInstanceModel = null;
            if (teiUid == null) {
                String generatedUid = codeGenerator.generate();
                trackedEntityInstanceModel =
                        TrackedEntityInstanceModel.builder()
                                .uid(generatedUid)
                                .created(currentDate)
                                .lastUpdated(currentDate)
                                .organisationUnit(orgUnit)
                                .trackedEntityType(teiType)
                                .state(State.TO_POST)
                                .build();

                if (briteDatabase.insert(TrackedEntityInstanceModel.TABLE,
                        trackedEntityInstanceModel.toContentValues()) < 0) {
                    String message = String.format(Locale.US, "Failed to insert new tracked entity " +
                                    "instance for organisationUnit=[%s] and trackedEntity=[%s]",
                            orgUnit, teiType);
                    return Observable.error(new SQLiteConstraintException(message));
                }

                for (String key : queryData.keySet()) {
                    TrackedEntityAttributeValueModel attributeValueModel =
                            TrackedEntityAttributeValueModel.builder()
                                    .created(currentDate)
                                    .lastUpdated(currentDate)
                                    .value(queryData.get(key))
                                    .trackedEntityAttribute(key)
                                    .trackedEntityInstance(generatedUid)
                                    .build();
                    if (briteDatabase.insert(TrackedEntityAttributeValueModel.TABLE,
                            attributeValueModel.toContentValues()) < 0) {
                        String message = String.format(Locale.US, "Failed to insert new trackedEntityAttributeValue " +
                                        "instance for organisationUnit=[%s] and trackedEntity=[%s]",
                                orgUnit, teiType);
                        return Observable.error(new SQLiteConstraintException(message));
                    }
                }

            } else {
                ContentValues dataValue = new ContentValues();

                // renderSearchResults time stamp
                dataValue.put(TrackedEntityInstanceModel.Columns.LAST_UPDATED,
                        BaseIdentifiableObject.DATE_FORMAT.format(currentDate));
                dataValue.put(TrackedEntityInstanceModel.Columns.STATE,
                        State.TO_POST.toString());

                if (briteDatabase.update(TrackedEntityInstanceModel.TABLE, dataValue,
                        TrackedEntityInstanceModel.Columns.UID + " = ? ", teiUid) <= 0) {
                    String message = String.format(Locale.US, "Failed to update tracked entity " +
                                    "instance for uid=[%s]",
                            teiUid);
                    return Observable.error(new SQLiteConstraintException(message));
                }
            }

            EnrollmentModel enrollmentModel = EnrollmentModel.builder()
                    .uid(codeGenerator.generate())
                    .created(currentDate)
                    .lastUpdated(currentDate)
                    .dateOfEnrollment(currentDate)
                    .program(programUid)
                    .organisationUnit(orgUnit)
                    .trackedEntityInstance(teiUid != null ? teiUid : trackedEntityInstanceModel.uid())
                    .enrollmentStatus(EnrollmentStatus.ACTIVE)
                    .state(State.TO_POST)
                    .build();

            if (briteDatabase.insert(EnrollmentModel.TABLE, enrollmentModel.toContentValues()) < 0) {
                String message = String.format(Locale.US, "Failed to insert new enrollment " +
                        "instance for organisationUnit=[%s] and program=[%s]", orgUnit, programUid);
                return Observable.error(new SQLiteConstraintException(message));
            }

            return Observable.just(enrollmentModel.uid());
        });
    }
}
