package teammates.datatransfer;

import java.util.Date;

import teammates.jdo.Evaluation;

public class EvaluationData {
	public String courseId;
	public String name;
	public String instructions = "";
	public Date startTime;
	public Date endTime;
	public double timeZone;
	public int gracePeriod;
	public boolean p2pEnabled;
	public boolean published = false;
	public boolean activated = false;

	public EvaluationData() {

	}

	public EvaluationData(Evaluation e) {
		this.courseId = e.getCourseID();
		this.name = e.getName();
		this.instructions = e.getInstructions();
		this.startTime = e.getStart();
		this.endTime = e.getDeadline();
		this.timeZone = e.getTimeZone();
		this.gracePeriod = e.getGracePeriod();
		this.p2pEnabled = e.isCommentsEnabled();
		this.published = e.isPublished();
		this.activated = e.isActivated();
	}

	public Evaluation toEvaluation() {
		Evaluation evaluation = new Evaluation(courseId, name, instructions, p2pEnabled,
				startTime, endTime, timeZone, gracePeriod);
		evaluation.setActivated(activated);
		evaluation.setPublished(published);
		return evaluation;
	}
}