package com.lethalskillzz.baker.data.local;

import android.annotation.SuppressLint;

import com.lethalskillzz.baker.data.model.Step;
import com.lethalskillzz.baker.utils.DbUtils;
import com.lethalskillzz.baker.data.RecipeDataSource;
import com.lethalskillzz.baker.data.model.Ingredient;
import com.lethalskillzz.baker.data.model.Recipe;
import com.squareup.sqlbrite.BriteDatabase;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Observable;

import static com.lethalskillzz.baker.data.local.db.IngredientPersistenceContract.IngredientEntry;
import static com.lethalskillzz.baker.data.local.db.RecipePersistenceContract.RecipeEntry;
import static com.lethalskillzz.baker.data.local.db.StepPersistenceContract.StepEntry;

/**
 * Created by ibrahimabdulkadir on 23/06/2017.
 */

@Singleton
public class RecipeLocalDataSource implements RecipeDataSource {

    private final BriteDatabase databaseHelper;

    @Inject
    public RecipeLocalDataSource(BriteDatabase briteDatabase) {
        this.databaseHelper = briteDatabase;
    }

    @Override
    public Observable<List<Recipe>> getRecipes() {

        rx.Observable<List<Recipe>> listObservable = databaseHelper
                .createQuery(RecipeEntry.TABLE_NAME, DbUtils.getSelectAllQuery(RecipeEntry.TABLE_NAME))
                .mapToOne(DbUtils::recipesFromCursor);

        return RxJavaInterop.toV2Observable(listObservable);
    }

    @Override
    public Observable<List<Ingredient>> getRecipeIngredients(int recipeId) {

        rx.Observable<List<Ingredient>> listObservable = databaseHelper
                .createQuery(IngredientEntry.TABLE_NAME,
                        DbUtils.getSelectByIdQuery(IngredientEntry.TABLE_NAME,
                                IngredientEntry.COLUMN_RECIPE_ID),
                        String.valueOf(recipeId))
                .mapToOne(DbUtils::ingredientsFromCursor);

        return RxJavaInterop.toV2Observable(listObservable);
    }

    @SuppressLint("NewApi")
    @Override
    public Observable<List<Ingredient>> getRecipeIngredients(String recipeName) {
        return getRecipes()
                .flatMap(Observable::fromIterable)
                .filter(recipe -> Objects.equals(recipe.name(), recipeName))
                .map(Recipe::id)
                .flatMap(this::getRecipeIngredients);
    }

    @Override
    public Observable<List<Step>> getRecipeSteps(int recipeId) {

        rx.Observable<List<Step>> listObservable = databaseHelper
                .createQuery(StepEntry.TABLE_NAME,
                        DbUtils.getSelectByIdQuery(StepEntry.TABLE_NAME,
                                StepEntry.COLUMN_RECIPE_ID),
                        String.valueOf(recipeId))
                .mapToOne(DbUtils::stepsFromCursor);

        return RxJavaInterop.toV2Observable(listObservable);
    }

    @Override
    public void saveRecipes(List<Recipe> recipes) {

        BriteDatabase.Transaction transaction = databaseHelper.newTransaction();

        try {
            deleteAllRecipes();

            for (Recipe recipe : recipes) {

                int id = recipe.id();

                for (Ingredient ingredient : recipe.ingredients()) {
                    databaseHelper.insert(IngredientEntry.TABLE_NAME,
                            DbUtils.ingredientToContentValues(ingredient, id));
                }

                for (Step step : recipe.steps()) {
                    databaseHelper.insert(StepEntry.TABLE_NAME,
                            DbUtils.stepToContentValues(step, id));
                }

                databaseHelper.insert(RecipeEntry.TABLE_NAME,
                        DbUtils.recipeToContentValues(recipe));
            }

            transaction.markSuccessful();

        } finally {
            transaction.end();
        }
    }

    private void deleteAllRecipes() {
        databaseHelper.delete(RecipeEntry.TABLE_NAME, null);
        databaseHelper.delete(StepEntry.TABLE_NAME, null);
        databaseHelper.delete(IngredientEntry.TABLE_NAME, null);
    }
}
