package io.github.clendy.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.clendy.multipleviewpager.MultiHorizontalViewPager;
import io.github.clendy.recyclertablayout.RecyclerTabLayout;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tab_layout)
    RecyclerTabLayout mTabLayout;
    @BindView(R.id.view_pager)
    MultiHorizontalViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


    }
}
