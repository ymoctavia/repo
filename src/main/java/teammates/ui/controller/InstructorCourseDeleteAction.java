package teammates.ui.controller;

import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.logic.api.GateKeeper;

/**
 * Action: Delete a course for an instructor
 */
public class InstructorCourseDeleteAction extends InstructorCoursesPageAction {
        
    @Override
    public ActionResult execute()
            throws EntityDoesNotExistException {

        String idOfCourseToDelete = getRequestParamValue(Const.ParamsNames.COURSE_ID);
        Assumption.assertNotNull(idOfCourseToDelete);
        
        new GateKeeper().verifyAccessible(
                logic.getInstructorForGoogleId(idOfCourseToDelete, account.googleId), 
                logic.getCourse(idOfCourseToDelete));

        /* Delete the course and setup status to be shown to user and admin */
        logic.deleteCourse(idOfCourseToDelete);
        String statusMessage = String.format(Const.StatusMessages.COURSE_DELETED, idOfCourseToDelete);
        statusToUser.add(statusMessage);
        statusToAdmin = "Course deleted: " + idOfCourseToDelete;

        if(isRedirectedToHomePage()){
            return createRedirectResult(Const.ActionURIs.INSTRUCTOR_HOME_PAGE);
        } else {
            return createRedirectResult(Const.ActionURIs.INSTRUCTOR_COURSES_PAGE);
        }
    }

    /**
     * Checks if the action is executed in homepage or 'Courses' pages based on its redirection
     */
    private boolean isRedirectedToHomePage() {
        String nextUrl = getRequestParamValue(Const.ParamsNames.NEXT_URL);
        boolean isHomePageUrl = (nextUrl != null && nextUrl.equals(Const.ActionURIs.INSTRUCTOR_HOME_PAGE));
        
        return isHomePageUrl;
    }
}
