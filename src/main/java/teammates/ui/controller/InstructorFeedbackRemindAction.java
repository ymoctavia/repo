package teammates.ui.controller;

import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.util.Const;
import teammates.logic.api.GateKeeper;

public class InstructorFeedbackRemindAction extends InstructorFeedbacksPageAction {

    @Override
    protected ActionResult execute() throws EntityDoesNotExistException {
        
        String courseId = getRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        
        new GateKeeper().verifyAccessible(
                logic.getInstructorForGoogleId(courseId, account.googleId),
                logic.getFeedbackSession(feedbackSessionName, courseId),
                false);
        
        logic.sendReminderForFeedbackSession(courseId,feedbackSessionName);
        
        statusToUser.add(Const.StatusMessages.FEEDBACK_SESSION_REMINDERSSENT);
        statusToAdmin = "Email sent out to all students who have not completed " +
                "Feedback Session <span class=\"bold\">(" + feedbackSessionName + ")</span> " +
                "of Course <span class=\"bold\">[" + courseId + "]</span>";
        
        return createRedirectResult(Const.ActionURIs.INSTRUCTOR_FEEDBACKS_PAGE);
    }

}
