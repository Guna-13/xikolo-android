package de.xikolo.controller.course;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import de.xikolo.R;
import de.xikolo.controller.BaseFragment;
import de.xikolo.controller.course.dialog.UnenrollDialog;
import de.xikolo.model.EnrollmentModel;
import de.xikolo.entities.Course;
import de.xikolo.util.Config;
import eu.inmite.android.lib.dialogs.ISimpleDialogListener;

public class CourseFragment extends BaseFragment implements ISimpleDialogListener {

    public final static String TAG = CourseFragment.class.getSimpleName();

    private static final String ARG_COURSE = "arg_course";

    private Course mCourse;

    public CourseFragment() {
        // Required empty public constructor
    }

    public static CourseFragment newInstance(Course course) {
        CourseFragment fragment = new CourseFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_COURSE, course);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCourse = getArguments().getParcelable(ARG_COURSE);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_course, container, false);

        // Initialize the ViewPager and set an adapter
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) layout.findViewById(R.id.tabs);
        ViewPager pager = (ViewPager) layout.findViewById(R.id.pager);

        pager.setAdapter(new CoursePagerAdapter(getChildFragmentManager()));

        // Bind the tabs to the ViewPager
        tabs.setViewPager(pager);

        return layout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.course, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.action_unenroll:
                UnenrollDialog.show(getActivity(), getChildFragmentManager(), this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPositiveButtonClicked(int i) {
        EnrollmentModel manager = new EnrollmentModel(getActivity(), jobManager);
        manager.deleteEnrollment(mCourse.id);
        getActivity().finish();
    }

    @Override
    public void onNegativeButtonClicked(int i) {
    }

    public class CoursePagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = {
                getString(R.string.tab_learnings),
                getString(R.string.tab_discussions),
                getString(R.string.tab_progress),
                getString(R.string.tab_announcements),
                getString(R.string.tab_rooms),
                getString(R.string.tab_details)
        };
        private FragmentManager mFragmentManager;

        public CoursePagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int position) {
            // Check if this Fragment already exists.
            // Fragment Name is saved by FragmentPagerAdapter implementation.
            String name = makeFragmentName(R.id.pager, position);
            Fragment fragment = mFragmentManager.findFragmentByTag(name);
            if (fragment == null) {
                switch (position) {
                    case 0:
                        fragment = CourseLearningsFragment.newInstance(mCourse);
                        break;
                    case 1:
                        fragment = EmbeddedWebViewFragment.newInstance(Config.URI_SAP + Config.COURSES + mCourse.id + "/" + Config.DISCUSSIONS);
                        break;
                    case 2:
                        fragment = ProgressFragment.newInstance(mCourse);
                        break;
                    case 3:
                        fragment = EmbeddedWebViewFragment.newInstance(Config.URI_SAP + Config.COURSES + mCourse.id + "/" + Config.ANNOUNCEMENTS);
                        break;
                    case 4:
                        fragment = EmbeddedWebViewFragment.newInstance(Config.URI_SAP + Config.COURSES + mCourse.id + "/" + Config.ROOMS);
                        break;
                    case 5:
                        fragment = EmbeddedWebViewFragment.newInstance(mCourse.url);
                        break;
                }
            }
            return fragment;
        }

        private String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
        }

    }

}
