// QuizFragment.java
// Contains the Flag Quiz logic
package com.hektor7.flagquiz;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;
import butterknife.OnClick;

public class QuizFragment extends Fragment {
    // String used when logging error messages
    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList; // flag file names
    private List<String> quizCountriesList; // countries in current quiz
    private Set<String> regionsSet; // world regions in current quiz
    private String correctAnswer; // correct country for the current flag
    private int totalGuesses; // number of guesses made
    private int correctAnswers; // number of correct guesses
    private int guessRows; // number of rows displaying guess Buttons
    private SecureRandom random; // used to randomize the quiz
    private Handler handler; // used to delay loading next flag
    private Animation shakeAnimation; // animation for incorrect guess

    @InjectView(R.id.questionNumberTextView)
    TextView questionNumberTextView; // shows current question #

    @InjectView(R.id.flagImageView)
    ImageView flagImageView; // displays a flag

    @InjectViews({R.id.row1LinearLayout, R.id.row2LinearLayout, R.id.row3LinearLayout})
    LinearLayout[] guessLinearLayouts; // rows of answer Buttons

    @InjectView(R.id.answerTextView)
    TextView answerTextView; // displays Correct! or Incorrect!


    // configures the QuizFragment when its View is created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view =
                inflater.inflate(R.layout.fragment_quiz, container, false);

        //Injectar vistas
        ButterKnife.inject(this, view);

        this.setupAtributes();
        this.setupListenerForGuessButtons();

        return view; // returns the fragment's view for display
    }

    /**
     * After the user guesses a correct flag, load the next flag
     */
    private void loadNextFlag() {
        String nextImage = this.obtainNextGuessAndSetAnswer();
        this.setupImageGuess(nextImage);
        this.shuffleAnswers();
        this.setupAnswersButtons();
    }

    /**
     * Set up randomly the answers buttons.
     */
    private void setupAnswersButtons() {
        // add 3, 6, or 9 guess Buttons based on the value of guessRows
        for (int row = 0; row < this.guessRows; row++) {
            // place Buttons in currentTableRow
            for (int column = 0;
                 column < this.guessLinearLayouts[row].getChildCount(); column++) {
                // get reference to Button to configure
                Button newGuessButton =
                        (Button) this.guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                // get country name and set it as newGuessButton's text
                String fileName = this.fileNameList.get((row * 3) + column);
                newGuessButton.setText(getCountryName(fileName));
            }
        }

        this.setupCorrectAnswerButton();

    }

    /**
     * Set up the correct answer's button.
     */
    private void setupCorrectAnswerButton() {
        // randomly replace one Button with the correct answer
        int row = this.random.nextInt(this.guessRows); // pick random row
        int column = this.random.nextInt(3); // pick random column
        LinearLayout randomRow = this.guessLinearLayouts[row]; // get the row
        String countryName = getCountryName(this.correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    /**
     * Shuffle answers and set location of the correct one
     */
    private void shuffleAnswers() {
        Collections.shuffle(this.fileNameList); // shuffle file names

        // put the correct answer at the end of fileNameList
        int correct = this.fileNameList.indexOf(this.correctAnswer);
        this.fileNameList.add(this.fileNameList.remove(correct));
    }

    /**
     * Set up the next image (flag)
     * @param nextImage
     *              Next image's name
     */
    private void setupImageGuess(String nextImage) {

        // extract the region from the next image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));
        // use AssetManager to load next image from assets folder
        AssetManager assets = getActivity().getAssets();
        try {
            // get an InputStream to the asset representing the next flag
            InputStream stream =
                    assets.open(region + "/" + nextImage + ".png");

            // load the asset as a Drawable and display on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            this.flagImageView.setImageDrawable(flag);
        } catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }
    }

    /**
     * Obtain next flag name and set answer.
     * @return Next flag name
     */
    private String obtainNextGuessAndSetAnswer() {

        // get file name of the next flag and remove it from the list
        String nextImage = this.quizCountriesList.remove(0);
        this.correctAnswer = nextImage; // update the correct answer
        this.answerTextView.setText(""); // clear answerTextView

        //FIXME: Revisar esto... puede que diese error aquí
        // display current question number
        this.questionNumberTextView.setText(
                getResources().getString(R.string.question,
                        (this.correctAnswers + 1), FLAGS_IN_QUIZ)
        );

        return nextImage;
    }

    /**
     * Parses the country flag file name and returns the country name
     *
     * @param name Country name
     * @return Parsed country name
     */
    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }


    @OnClick({R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9})
    public void clickAnswer(Button guessButton) {

        if (this.isCorrectAnswer(guessButton))
        {
            this.setupCorrectAnswerMessage(guessButton.getText().toString());
            this.disableButtons();

            if (this.allCorrectAnswers()) {
                //TODO: Continuar refactorizando
                // DialogFragment to display quiz stats and start new quiz
                DialogFragment quizResults =
                        new DialogFragment() {
                            // create an AlertDialog and return it
                            @Override
                            public Dialog onCreateDialog(Bundle bundle) {
                                AlertDialog.Builder builder =
                                        new AlertDialog.Builder(getActivity());
                                builder.setCancelable(false);

                                builder.setMessage(
                                        getResources().getString(R.string.results,
                                                totalGuesses, (1000 / (double) totalGuesses))
                                );

                                // "Reset Quiz" Button
                                builder.setPositiveButton(R.string.reset_quiz,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                resetQuiz();
                                            }
                                        } // end anonymous inner class
                                ); // end call to setPositiveButton

                                return builder.create(); // return the AlertDialog
                            } // end method onCreateDialog
                        }; // end DialogFragment anonymous inner class

                // use FragmentManager to display the DialogFragment
                quizResults.show(getFragmentManager(), "quiz results");
            } else // answer is correct but quiz is not over
            {
                // load the next flag after a 1-second delay
                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                loadNextFlag();
                            }
                        }, 2000
                ); // 2000 milliseconds for 2-second delay
            }
        } else // guess was incorrect
        {
            flagImageView.startAnimation(shakeAnimation); // play shake

            // display "Incorrect!" in red
            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(
                    getResources().getColor(R.color.incorrect_answer));
            guessButton.setEnabled(false); // disable incorrect answer
        }
    }

    /**
     * Returns if all answers are correct.
     *
     * @return true if all are correct.
     */
    private boolean allCorrectAnswers() {
        return this.correctAnswers == QuizFragment.FLAGS_IN_QUIZ;
    }

    private void setupCorrectAnswerMessage(String message) {
        ++correctAnswers; // increment the number of correct answers

        // display correct answer in green text
        answerTextView.setText(message + "!");
        answerTextView.setTextColor(
                getResources().getColor(R.color.correct_answer));
    }

    /**
     * Says if pressed button is the correct answer.
     *
     * @param guessButton Pressed button.
     * @return true if correct answer.
     */
    private boolean isCorrectAnswer(Button guessButton) {
        String guess = guessButton.getText().toString();
        String answer = getCountryName(correctAnswer);
        ++this.totalGuesses; // increment number of guesses the user has made

        return guess.equals(answer);
    }

    /**
     * Utility method that disables all answer Buttons
     */
    private void disableButtons() {
        for (int row = 0; row < this.guessRows; row++) {
            LinearLayout guessRow = this.guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }
    /**
     * Setup listener for guess buttons
     */
    private void setupListenerForGuessButtons() {
        // configure listeners for the guess Buttons
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                //button.setOnClickListener(this.guessButtonListener);
            }
        }
    }

    /**
     * Setup atributes
     */
    private void setupAtributes() {
        this.fileNameList = new ArrayList<String>();
        this.quizCountriesList = new ArrayList<String>();
        this.random = new SecureRandom();
        this.handler = new Handler();

        // set questionNumberTextView's text
        this.questionNumberTextView.setText(
                getResources().getString(R.string.question, 1, FLAGS_IN_QUIZ));

        this.setupAnimation();
    }

    /**
     * Setup shake animation
     */
    private void setupAnimation() {
        // load the shake animation that's used for incorrect answers
        this.shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
                R.anim.incorrect_shake);
        this.shakeAnimation.setRepeatCount(3); // animation repeats 3 times
    }

    /**
     * Update guessRows based on value in SharedPreferences
     *
     * @param sharedPreferences Preferences
     */
    public void updateGuessRows(SharedPreferences sharedPreferences) {
        // get the number of guess buttons that should be displayed
        String choices =
                sharedPreferences.getString(MainActivity.CHOICES, null);
        this.guessRows = Integer.parseInt(choices) / 3;

        // hide all guess button LinearLayouts
        for (LinearLayout layout : this.guessLinearLayouts)
            layout.setVisibility(View.INVISIBLE);

        // display appropriate guess button LinearLayouts
        for (int row = 0; row < this.guessRows; row++)
            this.guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }

    /**
     * Update world regions for quiz based on values in SharedPreferences
     *
     * @param sharedPreferences Preferences
     */
    public void updateRegions(SharedPreferences sharedPreferences) {
        this.regionsSet =
                sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    /**
     * Set up and start the next quiz
     */
    public void resetQuiz() {

        this.resetQuizAttributes();
        this.loadQuizCountriesList();

        this.loadNextFlag(); // start the quiz by loading the first flag
    }

    /**
     * Load the countries list to current quiz
     */
    private void loadQuizCountriesList() {
        this.loadFileNameList();
        this.addRandomCountriesToList();
    }

    /**
     * Add random countries to list
     */
    private void addRandomCountriesToList() {
        int flagCounter = 1;
        int numberOfFlags = this.fileNameList.size();

        // add FLAGS_IN_QUIZ random file names to the quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ) {
            int randomIndex = this.random.nextInt(numberOfFlags);

            // get the random file name
            String fileName = this.fileNameList.get(randomIndex);

            // if the region is enabled and it hasn't already been chosen
            if (!this.quizCountriesList.contains(fileName)) {
                this.quizCountriesList.add(fileName); // add the file to the list
                ++flagCounter;
            }
        }
    }

    /**
     * Load the countries filename list
     */
    private void loadFileNameList() {
        // use AssetManager to get image file names for enabled regions
        this.fileNameList.clear(); // empty list of image file names
        AssetManager assets = getActivity().getAssets();
        try {
            // loop through each region
            for (String region : this.regionsSet) {
                // get a list of all flag image files in this region
                String[] paths = assets.list(region);

                for (String path : paths)
                    this.fileNameList.add(path.replace(".png", ""));
            }
        } catch (IOException exception) {
            Log.e(TAG, "Error loading image file names", exception);
        }

    }

    /**
     * Set up initials values of quiz attributes.
     */
    private void resetQuizAttributes() {

        this.correctAnswers = 0; // reset the number of correct answers made
        this.totalGuesses = 0; // reset the total number of guesses the user made
        this.quizCountriesList.clear(); // clear prior list of quiz countries
    }

}


