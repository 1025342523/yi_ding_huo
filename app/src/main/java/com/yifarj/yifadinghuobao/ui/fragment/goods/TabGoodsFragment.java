package com.yifarj.yifadinghuobao.ui.fragment.goods;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.yifarj.yifadinghuobao.R;
import com.yifarj.yifadinghuobao.adapter.GoodsListAdapter;
import com.yifarj.yifadinghuobao.adapter.helper.AbsRecyclerViewAdapter;
import com.yifarj.yifadinghuobao.adapter.helper.EndlessRecyclerOnScrollListener;
import com.yifarj.yifadinghuobao.adapter.helper.HeaderViewRecyclerAdapter;
import com.yifarj.yifadinghuobao.model.entity.GoodsListEntity;
import com.yifarj.yifadinghuobao.model.helper.DataSaver;
import com.yifarj.yifadinghuobao.network.PageInfo;
import com.yifarj.yifadinghuobao.network.RetrofitHelper;
import com.yifarj.yifadinghuobao.network.utils.JsonUtils;
import com.yifarj.yifadinghuobao.ui.activity.shoppingcart.ShopDetailActivity;
import com.yifarj.yifadinghuobao.ui.activity.shoppingcart.ShoppingCartActivity;
import com.yifarj.yifadinghuobao.ui.fragment.base.BaseFragment;
import com.yifarj.yifadinghuobao.utils.AppInfoUtil;
import com.yifarj.yifadinghuobao.view.CustomEmptyView;
import com.yifarj.yifadinghuobao.view.SearchView;
import com.yifarj.yifadinghuobao.view.TitleView;
import com.yifarj.yifadinghuobao.view.utils.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * TabGoodsFragment
 *
 * @auther Czech.Yuan
 * @date 2017/5/12 15:07
 */
public class TabGoodsFragment extends BaseFragment {
    private static final int REQUEST_REFRESH = 10;

    @BindView(R.id.recycle)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty_view)
    CustomEmptyView mCustomEmptyView;

    //    @BindView(R.id.swipe_refresh_layout)
    //    SwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.titleView)
    TitleView titleView;

    @BindView(R.id.searchView)
    SearchView searchView;

    private boolean mIsRefreshing = false;


    private View loadMoreView;

    private HeaderViewRecyclerAdapter mHeaderViewRecyclerAdapter;

    private GoodsListAdapter mGoodsListAdapter;

    private List<GoodsListEntity.ValueEntity> goodsList = new ArrayList<>();

    private PageInfo pageInfo = new PageInfo();

    private int totalCount;


    @Override
    public int getLayoutResId() {
        return R.layout.fragment_goods;
    }

    @Override
    protected void finishCreateView(Bundle savedInstanceState) {
        LogUtils.e("TabGoodsFragment", "finishCreateView");
        //        new QBadgeView(getContext()).bindTarget(titleView.getImageViewContent()).setBadgeTextSize(10, true).setBadgeNumber(9).setGravityOffset(15, 20, true);
        isPrepared = true;
        lazyLoad();
        titleView.setRightIconClickListener(view -> {
            Intent intent = new Intent(getActivity(), ShoppingCartActivity.class);
            startActivityForResult(intent, REQUEST_REFRESH);
        });
        titleView.setLeftIconClickListener(view -> searchView.show());
        searchView.setOnSearchClickListener(new SearchView.OnSearchClickListener() {
            @Override
            public void onSearch(String keyword) {
                doSearch(keyword);
            }
        });
        searchView.setOnCancelListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchView.getListView().setAdapter(null);
                searchView.getEditText().setText("");
            }
        });
        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String result = s.toString();
                if (!StringUtils.isEmpty(result) && result.length() == 13) {
                    doSearch(result);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }


    private void doSearch(String keyword) {
        RetrofitHelper.getGoodsListAPI()
                .getGoodsList("ProductList", "", "(name like '%" + keyword + "%' or right(Code,4) like '%" + keyword + "%'" + "or Mnemonic like '%" + keyword + "%' or id in (select productid from TB_ProductBarcode where Barcode like '%" + keyword + "%' and len('" + keyword + "')>=8) and  status not in(4,8))", "[" + DataSaver.getMettingCustomerInfo().TraderId + "]", AppInfoUtil.getToken())
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GoodsListEntity>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull GoodsListEntity goodsListEntity) {
                        if (!goodsListEntity.HasError) {
                            if (goodsListEntity.Value != null && goodsListEntity.Value.size() > 0) {
                                searchView.getListView().setAdapter(new GoodsListAdapter(searchView.getListView(), goodsListEntity.Value, true));
                            } else {
                                ToastUtils.showShortSafe("无结果");
                            }
                        } else {
                            ToastUtils.showShortSafe("无结果");
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        ToastUtils.showShortSafe("当前网络不可用,请检查网络设置");
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    @Override
    protected void lazyLoad() {
        if (!isPrepared && !isVisible) {
            LogUtils.e("TabGoodsFragment", "lazyLoad（） false");
            return;
        }
        //        initRefreshLayout();
        pageInfo.PageIndex = 0;
        mIsRefreshing = false;
        goodsList.clear();
        loadData();
        initRecyclerView();
        isPrepared = false;
    }


    //    @Override
    //    protected void initRefreshLayout() {
    //        if (mSwipeRefreshLayout == null) {
    //            return;
    //        }
    //        mSwipeRefreshLayout.setColorSchemeColors(Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW);
    ////        mSwipeRefreshLayout.setColorSchemeResources(R.color.light_blue);
    //        mSwipeRefreshLayout.post(() -> {
    //
    //            mSwipeRefreshLayout.setRefreshing(true);
    //            mIsRefreshing = true;
    //            loadData();
    //        });

    //
    //        mSwipeRefreshLayout.setOnRefreshListener(() -> {
    //            try {
    //                pageInfo.PageIndex = 0;
    //                mIsRefreshing = false;
    //                goodsList.clear();
    //                loadData();
    //            } catch (Exception e) {
    //                e.printStackTrace();
    //            }
    //        });
    //    }

    @Override
    protected void initRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mGoodsListAdapter = new GoodsListAdapter(mRecyclerView, goodsList, true);
        mHeaderViewRecyclerAdapter = new HeaderViewRecyclerAdapter(mGoodsListAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST, R.drawable.recyclerview_divider_goods));
        mRecyclerView.setAdapter(mHeaderViewRecyclerAdapter);
        setRecycleNoScroll();
        createHeadView();
        createLoadMoreView();
        mRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener(mLinearLayoutManager) {

            @Override
            public void onLoadMore(int i) {
                mIsRefreshing = true;
                pageInfo.PageIndex++;
                loadData();
                loadMoreView.setVisibility(View.VISIBLE);
            }
        });

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mIsRefreshing;
            }
        });

        mGoodsListAdapter.setOnItemClickListener(new AbsRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, AbsRecyclerViewAdapter.ClickableViewHolder holder) {
                if (holder != null && position < goodsList.size()) {
                    Intent intent = new Intent(getActivity(), ShopDetailActivity.class);
                    intent.putExtra("shoppingId", goodsList.get(position).Id);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void loadData() {

        LogUtils.e("loadData", "获取商品列表数据");
        RetrofitHelper.getGoodsListAPI()
                .getGoodsList("ProductList", JsonUtils.serialize(pageInfo), "status  not in (4,8)", "[" + DataSaver.getMettingCustomerInfo().TraderId + "]", AppInfoUtil.getToken())
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(goodsListEntity -> {
                    if (goodsListEntity.PageInfo.PageIndex * goodsListEntity.PageInfo.PageLength >= goodsListEntity.PageInfo.TotalCount) {
                        loadMoreView.setVisibility(View.GONE);
                        mHeaderViewRecyclerAdapter.removeFootView();
                    }
                })
                .subscribe(new Observer<GoodsListEntity>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull GoodsListEntity goodsListEntity) {
                        if (!goodsListEntity.HasError) {
                            totalCount = goodsListEntity.PageInfo.TotalCount;
                            goodsList.addAll(goodsListEntity.Value);
                            finishTask();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        showEmptyView();
                        loadMoreView.setVisibility(View.GONE);
                        --pageInfo.PageIndex;
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    @Override
    protected void finishTask() {
        //        if (mSwipeRefreshLayout.isRefreshing()) {
        //            mSwipeRefreshLayout.setRefreshing(false);
        //        }
        mIsRefreshing = false;
        if (goodsList != null) {
            if (goodsList.size() == 0) {
                showEmptyView();
            } else {
                hideEmptyView();
            }
        }
        loadMoreView.setVisibility(View.GONE);
        LogUtils.e("Page：", pageInfo.PageIndex);
        LogUtils.e("ListSize", goodsList.size());
        if (!mGoodsListAdapter.onbind) {
            if (mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE || !mRecyclerView.isComputingLayout()) { // RecyclerView滑动过程中刷新数据导致的Crash(Android官方的一个Bug)
                mGoodsListAdapter.notifyDataSetChanged();
            }
        }
    }

    private void createHeadView() {

        View headView = LayoutInflater.from(getActivity())
                .inflate(R.layout.layout_search_archive_head_view, mRecyclerView, false);
        //        RecyclerView mHeadRecycler = (RecyclerView) headView.findViewById(
        //                R.id.search_archive_bangumi_head_recycler);
        //        mHeadRecycler.setHasFixedSize(false);
        //        mHeadRecycler.setNestedScrollingEnabled(false);
        //        mHeadRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        //        mGoodsListAdapter = new GoodsListAdapter(mHeadRecycler, goodsList);
        //        mHeadRecycler.setAdapter(mGoodsListAdapter);

        mHeaderViewRecyclerAdapter.addHeaderView(headView);
    }

    public void showEmptyView() {
        //        mSwipeRefreshLayout.setRefreshing(false);
        mCustomEmptyView.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        mCustomEmptyView.setEmptyImage(R.drawable.img_tips_error_load_error);
        mCustomEmptyView.setEmptyText("加载失败~(≧▽≦)~啦啦啦.");
    }


    public void hideEmptyView() {
        if (mCustomEmptyView == null || mRecyclerView == null) {
            return;
        }
        mCustomEmptyView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void createLoadMoreView() {
        loadMoreView = LayoutInflater.from(getActivity())
                .inflate(R.layout.layout_load_more, mRecyclerView, false);
        mHeaderViewRecyclerAdapter.addFooterView(loadMoreView);
        loadMoreView.setVisibility(View.GONE);
    }


    private void setRecycleNoScroll() {

        mRecyclerView.setOnTouchListener((v, event) -> mIsRefreshing);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        pageInfo.PageIndex = 0;
        goodsList.clear();
        loadData();
    }
}
