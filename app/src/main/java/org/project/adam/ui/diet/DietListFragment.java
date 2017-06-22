package org.project.adam.ui.diet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;
import org.project.adam.BaseFragment;
import org.project.adam.R;
import org.project.adam.persistence.Diet;
import org.project.adam.persistence.Lunch;

import java.io.IOException;
import java.util.List;

import timber.log.Timber;

@SuppressLint("Registered")
@EFragment(R.layout.fragment_diet_list)
public class DietListFragment extends BaseFragment implements DietListAdapter.DietSelectorListener {

    private static final int SELECT_FILE_RESULT_CODE = 666;

    DietListViewModel dietListViewModel;

    @Bean
    DietListAdapter listAdapter;
    @Bean
    DietUtils dietUtils;

    @ViewById(R.id.item_list)
    RecyclerView items;

    @Bean
    DietLoader dietLoader;

    @StringRes(R.string.select_file_to_load)
    protected String fileSelectionTitle;

    @StringRes(R.string.enter_diet_name_title)
    protected String enterDietNameTitle;

    @StringRes(R.string.enter_diet_name_content)
    protected String enterDietNameContent;

    @StringRes(R.string.enter_diet_name_ok_btn)
    protected String enterDietNameOk;

    @AfterViews
    void setUpRepoAdapter() {
        items.setAdapter(listAdapter);
        items.setHasFixedSize(true);
        items.addItemDecoration(new VerticalSpaceItemDecoration(32));
        listAdapter.setDietSelectorListener(this);
    }

    @AfterViews
    void setUpViewModel() {
        dietListViewModel = ViewModelProviders.of(this).get(DietListViewModel.class);
        dietListViewModel.getDiets()
            .observe(this, new Observer<List<Diet>>() {
                @Override
                public void onChanged(@Nullable List<Diet> diets) {
                    listAdapter.update(diets);
                }
            });
    }

    @Override
    public void onResume() {
        super.onResume();
        listAdapter.reload();
    }

    @Click(R.id.add_diet)
    public void onAddDietClick() {
        Timber.d("onAddDietClick - launching file selector intent");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(Intent.normalizeMimeType("text/plain"));
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, fileSelectionTitle),
            SELECT_FILE_RESULT_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        Timber.d("onActivityResult - %d - %d", requestCode, resultCode);
        if (requestCode == SELECT_FILE_RESULT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    Context context = getActivity();
                    List<Lunch> lunches = dietLoader.parseLunchesFromCsv(context.getContentResolver()
                        .openInputStream(data.getData()));
                    createDiet(context, lunches);
                } catch (IOException f) {
                    Timber.w(f, "Error while reading file");
                }
            } else {
                Timber.w("Invalid result got from file selection");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    public void dietSelected(Diet diet) {
        DietDetailActivity_.intent(this).dietId(diet.getId()).start();
    }


    public class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {

        private final int verticalSpaceHeight;

        public VerticalSpaceItemDecoration(int verticalSpaceHeight) {
            this.verticalSpaceHeight = verticalSpaceHeight;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            final int childAdapterPosition = parent.getChildAdapterPosition(view);
            if (childAdapterPosition == 0 && parent.getAdapter().getItemCount() > 0) {
                outRect.bottom = verticalSpaceHeight;
            }
        }
    }

    private void createDiet(final Context context, final List<Lunch> lunches) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(enterDietNameTitle);
        alert.setMessage(enterDietNameContent);
        final EditText userInput = new EditText(context);
        alert.setView(userInput);

        alert.setPositiveButton(enterDietNameOk, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String dietName = userInput.getText().toString();
                dialog.dismiss();
                dietListViewModel.createDiet(Diet.builder()
                        .name(dietName)
                        .build(),
                    lunches.toArray(new Lunch[lunches.size()]));
            }
        });

        AlertDialog alertDialog = alert.create();
        alertDialog.show();
    }
}