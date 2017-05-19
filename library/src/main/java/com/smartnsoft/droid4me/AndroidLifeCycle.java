package com.smartnsoft.droid4me;

import android.os.Bundle;

/**
 * @author Édouard Mercier
 * @since 2012.06.21
 */
public interface AndroidLifeCycle
{

  void onCreate(Bundle savedInstanceState);

  void onStart();

  void onResume();

  void onPause();

  void onStop();

  void onDestroy();

}
