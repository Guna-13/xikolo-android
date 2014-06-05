package de.xikolo.manager;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import de.xikolo.BuildConfig;
import de.xikolo.dataaccess.HttpRequest;
import de.xikolo.dataaccess.JsonRequest;
import de.xikolo.model.Enrollment;
import de.xikolo.util.BuildType;
import de.xikolo.util.Config;
import de.xikolo.util.Network;

public abstract class EnrollmentsManager {

    public static final String TAG = EnrollmentsManager.class.getSimpleName();

    private Context mContext;

    public EnrollmentsManager(Context context) {
        super();
        this.mContext = context;
    }

    public void requestEnrollments(boolean cache) {
        if (BuildConfig.buildType == BuildType.DEBUG)
            Log.i(TAG, "requestEnrollments() called | cache " + cache);

        Type type = new TypeToken<List<Enrollment>>() {
        }.getType();
        JsonRequest request = new JsonRequest(Config.API_SAP + Config.PATH_USER + Config.PATH_ENROLLMENTS, type, mContext) {
            @Override
            public void onRequestReceived(Object o) {
                if (o != null) {
                    List<Enrollment> enrolls = (List<Enrollment>) o;
                    if (BuildConfig.buildType == BuildType.DEBUG)
                        Log.i(TAG, "Enrollments received (" + enrolls.size() + ")");
                    onEnrollmentsRequestReceived(enrolls);
                } else {
                    if (BuildConfig.buildType == BuildType.DEBUG)
                        Log.w(TAG, "No Enrollments received");
                    onRequestCancelled();
                }
            }

            @Override
            public void onRequestCancelled() {
                if (BuildConfig.buildType == BuildType.DEBUG)
                    Log.w(TAG, "Enrollments Request cancelled");
                onEnrollmentsRequestCancelled();
            }
        };
        request.setCache(cache);
        request.setToken(TokenManager.getAccessToken(mContext));
        if (!Network.isOnline(mContext) && cache) {
            request.setCacheOnly(true);
        }
        request.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void createEnrollment(String id) {
        if (BuildConfig.buildType == BuildType.DEBUG)
            Log.i(TAG, "createEnrollment() called | id " + id);

        String query = "?course_id=" + id;

        HttpRequest request = new HttpRequest(Config.API_SAP + Config.PATH_USER + Config.PATH_ENROLLMENTS + query, mContext) {
            @Override
            public void onRequestReceived(Object o) {
                if (BuildConfig.buildType == BuildType.DEBUG)
                    Log.i(TAG, "Enrollment created");
                requestEnrollments(false);
            }

            @Override
            public void onRequestCancelled() {
                if (BuildConfig.buildType == BuildType.DEBUG)
                    Log.w(TAG, "Enrollment not created");
            }
        };
        request.setMethod(Config.HTTP_POST);
        request.setToken(TokenManager.getAccessToken(mContext));
        request.setCache(false);
        request.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void deleteEnrollment(String id) {
        if (BuildConfig.buildType == BuildType.DEBUG)
            Log.i(TAG, "deleteEnrollment() called | id " + id);

        HttpRequest request = new HttpRequest(Config.API_SAP + Config.PATH_USER + Config.PATH_ENROLLMENTS + id, mContext) {
            @Override
            public void onRequestReceived(Object o) {
                if (BuildConfig.buildType == BuildType.DEBUG)
                    Log.i(TAG, "Enrollment deleted");
                requestEnrollments(false);
            }

            @Override
            public void onRequestCancelled() {
                if (BuildConfig.buildType == BuildType.DEBUG)
                    Log.w(TAG, "Enrollment not deleted");
            }
        };
        request.setMethod(Config.HTTP_DELETE);
        request.setToken(TokenManager.getAccessToken(mContext));
        request.setCache(false);
        request.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public abstract void onEnrollmentsRequestReceived(List<Enrollment> enrolls);

    public abstract void onEnrollmentsRequestCancelled();

}
