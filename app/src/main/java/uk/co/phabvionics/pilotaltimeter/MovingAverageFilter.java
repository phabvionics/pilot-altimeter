package uk.co.phabvionics.pilotaltimeter;

public class MovingAverageFilter {
    private int mSampleLimit;
    private float[] mSamples;
    private int mNextSample;
    private int mNumSamples;
    private float mRunningSum;

    public MovingAverageFilter(int sampleLimit) {
        mSampleLimit = sampleLimit;
        mSamples = new float[sampleLimit];
    }

    public float Filter(float sample) {
        // If the sample buffer is not yet full,
        if (mNumSamples < mSampleLimit) {
            // Add the sample to the array.
            mSamples[mNextSample] = sample;
            // Add the sample to the running sum.
            mRunningSum += sample;
            // Increment the number of samples.
            mNumSamples++;
            // Else
        } else {
            // Subtract the value of the sample falling out of the array from the running sum.
            mRunningSum -= mSamples[mNextSample];
            // Overwrite the sample in the array with the new sample.
            mSamples[mNextSample] = sample;
            // Add the value of the new sample to the running sum.
            mRunningSum += sample;
        }
        // Point to the next element of the samples array.
        mNextSample++;
        if (mNextSample == mSampleLimit) {
            mNextSample = 0;
        }
        // Divide the running sum by the number of samples and return it.
        return mRunningSum / mNumSamples;
    }
}
