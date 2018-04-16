package de.xikolo.network

import de.xikolo.models.*
import retrofit2.Call
import retrofit2.http.*

interface ApiServiceInterface {

    // Base Request for Health Checks on Startup

    @HEAD("./")
    fun base(): Call<Void>

    // Course

    @GET("courses")
    fun listCourses(): Call<Array<Course.JsonModel>>

    @GET("courses?include=user_enrollment")
    fun listCoursesWithEnrollments(): Call<Array<Course.JsonModel>>

    @GET("courses/{id}")
    fun getCourse(@Path("id") id: String): Call<Course.JsonModel>

    @GET("courses/{id}?include=user_enrollment")
    fun getCourseWithEnrollment(@Path("id") id: String): Call<Course.JsonModel>

    @GET("courses/{id}?include=sections")
    fun getCourseWithSections(@Path("id") id: String): Call<Course.JsonModel>

    // Enrollment

    @GET("enrollments")
    fun listEnrollments(): Call<Array<Enrollment.JsonModel>>

    @POST("enrollments")
    fun createEnrollment(@Body enrollment: Enrollment.JsonModel): Call<Enrollment.JsonModel>

    @PATCH("enrollments/{id}")
    fun updateEnrollment(@Path("id") id: String, @Body enrollment: Enrollment.JsonModel): Call<Enrollment.JsonModel>

    @DELETE("enrollments/{id}")
    fun deleteEnrollment(@Path("id") id: String): Call<Void>

    // Section

    @GET("course-sections?include=items")
    fun listSectionsWithItemsForCourse(@Query("filter[course]") courseId: String): Call<Array<Section.JsonModel>>

    // Item

    @GET("course-items/{id}?include=content")
    fun getItemWithContent(@Path("id") id: String): Call<Item.JsonModel>

    @GET("course-items?include=content")
    fun listItemsWithContentForSection(@Query("filter[section]") sectionId: String): Call<Array<Item.JsonModel>>

    @PATCH("course-items/{id}")
    fun updateItem(@Path("id") id: String, @Body item: Item.JsonModel): Call<Item.JsonModel>

    // Progress

    @GET("course-progresses/{id}?include=section-progresses")
    fun getCourseProgressWithSections(@Path("id") id: String): Call<CourseProgress.JsonModel>

    // Subtitle

    @GET("subtitle-tracks?include=cues")
    fun listSubtitlesWithCuesForVideo(@Query("filter[video]") videoId: String): Call<Array<SubtitleTrack.JsonModel>>

    // User and Profile

    @GET("users/me?include=profile")
    fun getUserWithProfile(): Call<User.JsonModel>

    // Announcement

    @GET("announcements?filter[global]=true")
    fun listGlobalAnnouncements(): Call<Array<Announcement.JsonModel>>

    @GET("announcements")
    fun listCourseAnnouncements(@Query("filter[course]") courseId: String): Call<Array<Announcement.JsonModel>>

    @PATCH("announcements/{id}")
    fun updateAnnouncement(@Path("id") id: String, @Body item: Announcement.JsonModel): Call<Announcement.JsonModel>

    // Token

    @FormUrlEncoded
    @POST("authenticate")
    fun createToken(@Field("email") email: String, @Field("password") password: String): Call<AccessToken>

    // SAP 5th Birthday Special

    @GET("osap_5th_birthday")
    fun getBirthdayVoucher(): Call<BirthdayVoucher>

}
