public interface ProgressBarView {

    /**
     * Updates the ProgressMonitor.
     *
     * @param progressPercentage The percentage to be displayed on the ProgressMonitor.
     * @return Returns false if the task has been cancelled
     */
    boolean updateProgress(int progressPercentage, long timeLeft);

}
