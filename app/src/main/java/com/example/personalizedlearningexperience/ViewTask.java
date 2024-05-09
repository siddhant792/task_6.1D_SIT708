package com.example.personalizedlearningexperience;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import com.android.volley.toolbox.JsonObjectRequest;

public class ViewTask extends AppCompatActivity {

    TextView tv_task_title, tv_task_description;
    private RequestQueue requestQueue;
    HashMap<String, List<String>> questionsAndOptions;
    HashMap<String, String> questionsAndCorrectAnswers;
    String desc;
    RecyclerView recyclerViewQuestions;
    Button buttonSubmitQues;

    List<String> checkedOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_task);
        tv_task_title = findViewById(R.id.tv_task_title);
        tv_task_description = findViewById(R.id.tv_task_description);
        recyclerViewQuestions = findViewById(R.id.recyclerViewQuestions);
        buttonSubmitQues = findViewById(R.id.buttonSubmitQues);
        checkedOptions = new ArrayList<>();
        questionsAndOptions = new LinkedHashMap<>();
        questionsAndCorrectAnswers = new LinkedHashMap<>();
        String title = getIntent().getStringExtra("title");
        desc = getIntent().getStringExtra("desc");
        tv_task_title.setText(title);
        tv_task_description.setText(desc);
        requestQueue = Volley.newRequestQueue(this);

        getQuizData("http://localhost:5000/getQuiz?topic=" + encodeString(title));

        QuestionAdapter questionAdapter = new QuestionAdapter(getConvertedList());
        recyclerViewQuestions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewQuestions.setAdapter(questionAdapter);
        buttonSubmitQues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkCompletion()) {
                    Toast.makeText(ViewTask.this, "Please attempt all the questions", Toast.LENGTH_SHORT).show();
                }else {
                    String[] quesAns = new String[questionsAndCorrectAnswers.size()];
                    int i = 0;
                    for (Map.Entry<String, String> entry : questionsAndCorrectAnswers.entrySet()) {
                        quesAns[i++] = entry.getKey() + "@" + entry.getValue();
                    }
                    Intent intent = new Intent(ViewTask.this, ViewResult.class);
                    intent.putExtra("ques", quesAns);
                    startActivity(intent);
                }
            }
        });
    }

    public boolean checkCompletion() {
        for(String e: checkedOptions) {
            if(e.isEmpty()) return false;
        }
        return true;
    }

    public List<List<String>> getConvertedList() {
        List<List<String>> combinedList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : questionsAndOptions.entrySet()) {
            List<String> tempList = new ArrayList<>();
            tempList.add(entry.getKey());
            tempList.addAll(entry.getValue());
            combinedList.add(tempList);
        }
        return combinedList;
    }

    private class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.TaskViewHolder> {

        List<List<String>> questionsAndOptions;

        public QuestionAdapter(List<List<String>> questionsAndOptions) {
            this.questionsAndOptions = questionsAndOptions;
        }

        @NonNull
        @Override
        public QuestionAdapter.TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question, parent, false);
            return new QuestionAdapter.TaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull QuestionAdapter.TaskViewHolder holder, int position) {
            List<String> questions = questionsAndOptions.get(position);
            holder.bind(questions);
        }

        @Override
        public int getItemCount() {
            return questionsAndOptions.size();
        }

        public class TaskViewHolder extends RecyclerView.ViewHolder {
            TextView tv_task_title;
            RadioButton rd_1, rd_2, rd_3, rd_4;
            RadioGroup rd_gp;

            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                tv_task_title = itemView.findViewById(R.id.tv_task_title);
                rd_1 = itemView.findViewById(R.id.rd_1);
                rd_2 = itemView.findViewById(R.id.rd_2);
                rd_3 = itemView.findViewById(R.id.rd_3);
                rd_4 = itemView.findViewById(R.id.rd_4);
                rd_gp = itemView.findViewById(R.id.rd_gp);

                rd_gp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        RadioButton checkedRadioButton = findViewById(checkedId);
                        if (checkedRadioButton != null) {
                            String checkedOptionText = checkedRadioButton.getText().toString();
                            checkedOptions.set(getAdapterPosition(), checkedOptionText);
                        }
                    }
                });
            }

            public void bind( List<String> questions) {
                int pos = getAdapterPosition() + 1;
                tv_task_title.setText(pos + ". " + questions.get(0));
                rd_1.setText(questions.get(1));
                rd_2.setText(questions.get(2));
                rd_3.setText(questions.get(3));
                rd_4.setText(questions.get(4));
            }
        }
    }

    public static String encodeString(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void getQuizData(String url) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray quizArray = response.getJSONArray("quiz");

                            for (int i = 0; i < quizArray.length(); i++) {
                                JSONObject quizObject = quizArray.getJSONObject(i);
                                String question = quizObject.getString("question");
                                char correctAnswer = quizObject.getString("correct_answer").charAt(0);
                                JSONArray optionsArray = quizObject.getJSONArray("options");

                                List<String> options = new ArrayList<>();
                                for (int j = 0; j < optionsArray.length(); j++) {
                                    options.add(optionsArray.getString(j));
                                }

                                questionsAndOptions.put(question, options);
                                questionsAndCorrectAnswers.put(question, options.get(correctAnswer - 'A'));
                                checkedOptions.add("");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        );

        requestQueue.add(jsonObjectRequest);
    }
}