package com.bignerdranch.android.pingpong;

import android.databinding.DataBindingUtil;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bignerdranch.android.pingpong.databinding.ActivityScoreBinding;
import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;

import rx.Observable;

import static rx.Observable.combineLatest;

public class ScoreActivity extends AppCompatActivity {
    private static final String TAG = "ScoreActivity";
    private Preference<Integer> mPlayer1Score;
    private Preference<Integer> mPlayer2Score;

    public enum GameState {
        Playing("playing..."),
        MatchPoint1("Game point: Player 1"),
        MatchPoint2("Game point: Player 2"),
        Victory1("Player 1 wins!", false),
        Victory2("Player 2 wins!", false)
        ;

        private final String mDescription;
        private final boolean mIsPlaying;

        GameState(String description, boolean isPlaying) {
            mDescription = description;
            mIsPlaying = isPlaying;
        }

        GameState(String description) {
            this(description, true);
        }

        public String getDescription() {
            return mDescription;
        }

        public boolean isPlaying() {
            return mIsPlaying;
        }

        public static GameState getGameState(int score1, int score2) {
            if (score1 < 20 && score2 < 20 || score1 == score2) {
                return GameState.Playing;
            } else if (score1 >= 21 && score1 - score2 > 1) {
                return GameState.Victory1;
            } else if (score2 >= 21 && score2 - score1 > 1) {
                return GameState.Victory2;
            } else if (getGameState(score1 + 1, score2) == GameState.Victory1) {
                return GameState.MatchPoint1;
            } else if (getGameState(score1, score2 + 1) == GameState.Victory2) {
                return GameState.MatchPoint2;
            } else {
                return GameState.Playing;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);

        ActivityScoreBinding binding = DataBindingUtil
                .setContentView(this, R.layout.activity_score);

        RxSharedPreferences preferences = RxSharedPreferences.create(
                PreferenceManager.getDefaultSharedPreferences(this)
        );

        mPlayer1Score = preferences
                .getInteger("player1Score", 0);
        mPlayer2Score = preferences
                .getInteger("player2Score", 0);

        // Display scores for players 1 and 2
        mPlayer1Score.asObservable()
                .map(value -> value.toString())
                .subscribe(RxTextView.text(binding.score1TextView));
        mPlayer2Score.asObservable()
                .map(value -> value.toString())
                .subscribe(RxTextView.text(binding.score2TextView));

        Observable<GameState> gameState =
                combineLatest(mPlayer1Score.asObservable(), mPlayer2Score.asObservable(),
                GameState::getGameState);

        // Display description of game state at top of screen
        gameState
                .map(state -> state.getDescription())
                .doOnNext(RxTextView.text(binding.victoryTextView))
                .subscribe();

        // Disable buttons when not playing
        gameState
                .map(state -> state.isPlaying())
                .doOnNext(RxView.enabled(binding.score1PointButton))
                .doOnNext(RxView.enabled(binding.score2PointButton))
                .subscribe();

        RxView.clicks(binding.score1PointButton)
                .subscribe(view -> {
                    increment(mPlayer1Score);
                });
        RxView.clicks(binding.score2PointButton)
                .subscribe(view -> {
                    increment(mPlayer2Score);
                });
        RxView.clicks(binding.newGameButton)
                .subscribe(view -> {
                    mPlayer1Score.set(0);
                    mPlayer2Score.set(0);
                });
    }

    private void increment(Preference<Integer> score) {
        score.set(score.get() + 1);
    }
}
