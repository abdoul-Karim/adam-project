package org.project.adam.ui.diet;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.project.adam.persistence.Diet;
import org.project.adam.util.ui.RecyclerViewAdapterBase;
import org.project.adam.util.ui.ViewWrapper;

import java.util.ArrayList;
import java.util.Collection;

import lombok.Setter;

@EBean
class DietListAdapter extends RecyclerViewAdapterBase<Diet, DietItemView> {

    public static final int CURRENT_TYPE = 1;
    public static final int OTHER_TYPE = 2;

    interface DietSelectorListener {
        void dietSelected(Diet diet);
    }

    @RootContext
    Context context;

    @Bean
    DietUtils dietUtils;

    @Setter
    DietSelectorListener dietSelectorListener;

    @Override
    protected DietItemView onCreateItemView(ViewGroup parent, int viewType) {
        return DietItemView_.build(context);
    }

    @Override
    public void onBindViewHolder(ViewWrapper<DietItemView> holder, int position) {
        final Diet diet = items.get(position);
        final DietItemView view = holder.getView();
        view.bind(diet);

        if (dietSelectorListener == null) {
            return;
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dietSelectorListener.dietSelected(diet);
            }
        });
    }

    public int getItemViewType(int position) {
        Diet diet = items.get(position);
        return dietUtils.isCurrent(diet.getId()) ? CURRENT_TYPE : OTHER_TYPE;
    }


    public void reload (){
        // ugly
        update(items);
    }

    @Override
    public void update(Collection<Diet> diets) {

        ArrayList<Diet> firstCurrentDietList = new ArrayList<>();

        for (Diet diet : diets) {
            if (dietUtils.isCurrent(diet)) {
                firstCurrentDietList.add(0, diet);
            } else {
                firstCurrentDietList.add(diet);
            }
        }

        super.update(firstCurrentDietList);
    }
}