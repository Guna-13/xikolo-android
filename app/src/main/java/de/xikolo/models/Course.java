package de.xikolo.models;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.moshi.Json;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.xikolo.App;
import de.xikolo.R;
import de.xikolo.config.Feature;
import de.xikolo.models.base.JsonAdapter;
import de.xikolo.models.base.RealmAdapter;
import de.xikolo.models.dao.ChannelDao;
import de.xikolo.models.dao.EnrollmentDao;
import de.xikolo.utils.LanguageUtil;
import de.xikolo.utils.extensions.DateUtil;
import de.xikolo.utils.extensions.DisplayUtil;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import moe.banana.jsonapi2.HasMany;
import moe.banana.jsonapi2.HasOne;
import moe.banana.jsonapi2.JsonApi;
import moe.banana.jsonapi2.Resource;

public class Course extends RealmObject implements JsonAdapter<Course.JsonModel> {

    @PrimaryKey
    public String id;

    public String title;

    public String slug;

    @Nullable
    public Date startDate;

    @Nullable
    public Date endDate;

    public String shortAbstract;

    public String description;

    public String imageUrl;

    public String language;

    public String status;

    public String classifiers;

    public String teachers;

    public boolean accessible;

    public boolean enrollable;

    public boolean hidden;

    public boolean external;

    public String externalUrl;

    public String policyUrl;

    public CourseCertificates certificates;

    public VideoStream teaserStream;

    public boolean onDemand;

    public String enrollmentId;

    public String channelId;

    public Enrollment getEnrollment() {
        return EnrollmentDao.Unmanaged.find(enrollmentId);
    }

    public Channel getChannel() {
        return ChannelDao.Unmanaged.find(channelId);
    }

    public boolean isEnrolled() {
        return enrollmentId != null;
    }

    @Override
    public JsonModel convertToJsonResource() {
        Course.JsonModel model = new Course.JsonModel();
        model.setId(id);
        model.title = title;
        model.slug = slug;
        model.startDate = DateUtil.getFormattedString(startDate);
        model.endDate = DateUtil.getFormattedString(endDate);
        model.shortAbstract = shortAbstract;
        model.description = description;
        model.imageUrl = imageUrl;
        model.language = language;
        model.status = status;
        model.teachers = teachers;
        model.accessible = accessible;
        model.enrollable = enrollable;
        model.hidden = hidden;
        model.external = external;
        model.externalUrl = externalUrl;
        model.policyUrl = policyUrl;
        model.onDemand = onDemand;
        model.certificates = certificates;
        model.teaserStream = teaserStream;

        model.classifiers = new Gson().fromJson(classifiers, new TypeToken<Map<String, List<String>>>() {}.getType());

        if (enrollmentId != null) {
            model.enrollment = new HasOne<>(new Enrollment.JsonModel().getType(), enrollmentId);
        }

        if (channelId != null) {
            model.channel = new HasOne<>(new Channel.JsonModel().getType(), channelId);
        }

        return model;
    }

    public String getFormattedDate() {
        Context context = App.getInstance();

        DateFormat dateOut;
        if (DisplayUtil.is7inchTablet(context)) {
            dateOut = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        } else {
            dateOut = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        }

        if (DateUtil.isPast(endDate)) {
            return context.getString(R.string.course_date_self_paced);
        }

        if (DateUtil.isPast(startDate) && endDate == null) {
            if (Feature.INSTANCE.enabled("show_dates_for_never_ending_courses")) {
                return String.format(context.getString(R.string.course_date_since), dateOut.format(startDate));
            } else {
                return context.getString(R.string.course_date_self_paced);
            }
        }

        if (DateUtil.isFuture(startDate) && endDate == null) {
            if (Feature.INSTANCE.enabled("show_dates_for_never_ending_courses")) {
                return String.format(context.getString(R.string.course_date_beginning), dateOut.format(startDate));
            } else {
                return context.getString(R.string.course_date_coming_soon);
            }
        }

        if (startDate != null && endDate != null) {
            return dateOut.format(startDate) + " - " + dateOut.format(endDate);
        }

        return context.getString(R.string.course_date_coming_soon);
    }

    public String getLanguageAsNativeName() {
        return LanguageUtil.INSTANCE.toNativeName(language);
    }

    @JsonApi(type = "courses")
    public static class JsonModel extends Resource implements RealmAdapter<Course> {

        public String title;

        public String slug;

        @Json(name = "start_at")
        public String startDate;

        @Json(name = "end_at")
        public String endDate;

        @Json(name = "abstract")
        public String shortAbstract;

        public String description;

        @Json(name = "image_url")
        public String imageUrl;

        public String language;

        public String status;

        public Map<String, List<String>> classifiers;

        public String teachers;

        public boolean accessible;

        public boolean enrollable;

        public boolean hidden;

        public boolean external;

        @Json(name = "external_url")
        public String externalUrl;

        @Json(name = "policy_url")
        public String policyUrl;

        public CourseCertificates certificates;

        @Json(name = "teaser_stream")
        public VideoStream teaserStream;

        @Json(name = "on_demand")
        public boolean onDemand;

        @Json(name = "user_enrollment")
        public HasOne<Enrollment.JsonModel> enrollment;

        @Json(name = "sections")
        public HasMany<Section.JsonModel> sections;

        @Json(name = "channel")
        public HasOne<Section.JsonModel> channel;

        @Override
        public Course convertToRealmObject() {
            Course course = new Course();
            course.id = getId();
            course.title = title;
            course.slug = slug;
            course.startDate = DateUtil.getAsDate(startDate);
            course.endDate = DateUtil.getAsDate(endDate);
            course.shortAbstract = shortAbstract;
            course.description = description;
            course.imageUrl = imageUrl;
            course.language = language;
            course.status = status;
            course.teachers = teachers;
            course.accessible = accessible;
            course.enrollable = enrollable;
            course.hidden = hidden;
            course.external = external;
            course.externalUrl = externalUrl;
            course.policyUrl = policyUrl;
            course.certificates = certificates;
            course.teaserStream = teaserStream;
            course.onDemand = onDemand;

            course.classifiers = new Gson().toJson(classifiers);

            if (enrollment != null) {
                course.enrollmentId = enrollment.get().getId();
            }

            if (channel != null) {
                course.channelId = channel.get().getId();
            }

            return course;
        }

    }

}
