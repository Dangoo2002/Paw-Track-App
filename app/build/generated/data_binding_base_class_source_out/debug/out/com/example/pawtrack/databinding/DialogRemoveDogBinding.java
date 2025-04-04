// Generated by view binder compiler. Do not edit!
package com.example.pawtrack.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.pawtrack.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class DialogRemoveDogBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final Spinner spinnerDogs;

  private DialogRemoveDogBinding(@NonNull LinearLayout rootView, @NonNull Spinner spinnerDogs) {
    this.rootView = rootView;
    this.spinnerDogs = spinnerDogs;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static DialogRemoveDogBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static DialogRemoveDogBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.dialog_remove_dog, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static DialogRemoveDogBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.spinner_dogs;
      Spinner spinnerDogs = ViewBindings.findChildViewById(rootView, id);
      if (spinnerDogs == null) {
        break missingId;
      }

      return new DialogRemoveDogBinding((LinearLayout) rootView, spinnerDogs);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
