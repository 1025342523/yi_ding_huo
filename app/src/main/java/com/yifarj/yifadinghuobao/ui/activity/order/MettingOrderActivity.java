package com.yifarj.yifadinghuobao.ui.activity.order;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.facebook.stetho.common.LogUtil;
import com.jakewharton.rxbinding2.view.RxView;
import com.raizlabs.android.dbflow.rx2.language.RXSQLite;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.wx.wheelview.widget.WheelView;
import com.yifarj.yifadinghuobao.R;
import com.yifarj.yifadinghuobao.database.model.GoodsPropertyModel;
import com.yifarj.yifadinghuobao.database.model.GoodsPropertyModel_Table;
import com.yifarj.yifadinghuobao.database.model.GoodsUnitModel;
import com.yifarj.yifadinghuobao.database.model.GoodsUnitModel_Table;
import com.yifarj.yifadinghuobao.database.model.ReturnGoodsUnitModel;
import com.yifarj.yifadinghuobao.database.model.ReturnGoodsUnitModel_Table;
import com.yifarj.yifadinghuobao.database.model.ReturnListItemModel;
import com.yifarj.yifadinghuobao.database.model.SaleGoodsItemModel;
import com.yifarj.yifadinghuobao.model.entity.CreateOrderEntity;
import com.yifarj.yifadinghuobao.model.entity.MettingLoginEntity;
import com.yifarj.yifadinghuobao.model.entity.ProductPropertyListEntity;
import com.yifarj.yifadinghuobao.model.entity.ProductUnitEntity;
import com.yifarj.yifadinghuobao.model.entity.ReceiveMethodListEntity;
import com.yifarj.yifadinghuobao.model.entity.SaleGoodsItem;
import com.yifarj.yifadinghuobao.model.entity.TraderEntity;
import com.yifarj.yifadinghuobao.model.helper.DataSaver;
import com.yifarj.yifadinghuobao.network.RetrofitHelper;
import com.yifarj.yifadinghuobao.network.utils.JsonUtils;
import com.yifarj.yifadinghuobao.ui.activity.base.BaseActivity;
import com.yifarj.yifadinghuobao.utils.AppInfoUtil;
import com.yifarj.yifadinghuobao.utils.CommonUtil;
import com.yifarj.yifadinghuobao.utils.DateUtil;
import com.yifarj.yifadinghuobao.utils.NumberUtil;
import com.yifarj.yifadinghuobao.utils.ZipUtil;
import com.yifarj.yifadinghuobao.view.CustomEditItem;
import com.yifarj.yifadinghuobao.view.CustomEditItemUnderline;
import com.yifarj.yifadinghuobao.view.CustomEmptyView;
import com.yifarj.yifadinghuobao.view.LoadingDialog;
import com.yifarj.yifadinghuobao.view.TitleView;
import com.yifarj.yifadinghuobao.view.WheelViewBottomDialog;
import com.yifarj.yifadinghuobao.view.listener.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import io.reactivex.Flowable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


/**
 * 订货单
 *
 * @auther Czech.Yuan
 * @date 2017/5/21 10:01
 */
public class MettingOrderActivity extends BaseActivity {

    @BindView(R.id.titleView)
    TitleView titleView;

    @BindView(R.id.scrollView)
    ScrollView scrollView;

    @BindView(R.id.rlBottomView)
    RelativeLayout rlBottomView;

    @BindView(R.id.tvTotal)
    TextView tvTotal;

    @BindView(R.id.tvTotalPrice)
    TextView tvTotalPrice;

    @BindView(R.id.rlSave)
    RelativeLayout rlSave;

    @BindView(R.id.tvSave)
    TextView tvSave;

    @BindView(R.id.llContentView)
    LinearLayout llContentView;

    @BindView(R.id.ciName)
    CustomEditItem ciName;

    @BindView(R.id.ciContact)
    CustomEditItem ciContact;

    @BindView(R.id.ciPhone)
    CustomEditItem ciPhone;

    @BindView(R.id.ciGoodsListCount)
    CustomEditItem ciGoodsListCount;

    @BindView(R.id.ciDeliveryDate)
    CustomEditItem ciDeliveryDate;

    @BindView(R.id.ciDate)
    CustomEditItem ciDate;

    @BindView(R.id.ciAddress)
    CustomEditItemUnderline ciAddress;

    @BindView(R.id.ciReceiveMethod)
    CustomEditItemUnderline ciReceiveMethod;

    @BindView(R.id.ciRemark)
    CustomEditItem ciRemark;

    @BindView(R.id.empty_view)
    CustomEmptyView emptyView;

    private CreateOrderEntity.ValueEntity orderInfo;
    private List<ReceiveMethodListEntity.ValueEntity> receiveMethodList;
    private List<SaleGoodsItem.ValueEntity> mItemData = new ArrayList<>();
    private List<TraderEntity.ValueEntity.TraderDeliveryAddressListEntity> mAddressList = new ArrayList<>();
    private boolean isCreate;
    private int mPosition;
    private int orderId;
    private boolean refresh;
    private int orderCount, orderTotal, saleType = 0;


    @Override
    public int getLayoutId() {
        return R.layout.activity_metting_order;
    }


    @Override
    public void initViews(Bundle savedInstanceState) {
        loadData();

        init();

    }

    @Override
    public void loadData() {
        getIntentExtra();
        TraderEntity.ValueEntity traderInfo = DataSaver.getTraderInfo();
        if (traderInfo == null) {
            mAddressList = DataSaver.getTraderInfo().TraderDeliveryAddressList;
        }
        if (isCreate) {
            if (saleType == 1) {
                RXSQLite.rx(SQLite.select()
                        .from(ReturnListItemModel.class))
                        .queryList()
                        .subscribe(new Consumer<List<ReturnListItemModel>>() {
                            @Override
                            public void accept(@NonNull List<ReturnListItemModel> returnListItemModels) throws Exception {
                                if (returnListItemModels != null && returnListItemModels.size() > 0) {
                                    Flowable.fromIterable(returnListItemModels)
                                            .forEach(new Consumer<ReturnListItemModel>() {
                                                @Override
                                                public void accept(@NonNull ReturnListItemModel returnListItemModel) throws Exception {
                                                    SaleGoodsItem.ValueEntity mItem = new SaleGoodsItem.ValueEntity();
                                                    mItem.ProductProperyId1 = returnListItemModel.ParentProperyId1;
                                                    mItem.ProductProperyId2 = returnListItemModel.ParentProperyId2;
                                                    mItem.ProperyId1 = returnListItemModel.ProperyId1;
                                                    mItem.ProperyId2 = returnListItemModel.ProperyId2;
                                                    mItem.PriceSystemId = returnListItemModel.PriceSystemId;
                                                    mItem.CurrentPrice = returnListItemModel.CurrentPrice;
                                                    mItem.TotalPrice = returnListItemModel.CurrentPrice;
                                                    mItem.ImagePath = returnListItemModel.Path;
                                                    mItem.ProductName = returnListItemModel.ProductName;
                                                    mItem.ActualPrice = returnListItemModel.UnitPrice;
                                                    mItem.ActualUnitPrice = returnListItemModel.BasicUnitPrice;
                                                    mItem.ProductUnitName = returnListItemModel.ProductUnitName;
                                                    mItem.BasicUnitPrice = returnListItemModel.BasicUnitPrice;
                                                    mItem.UnitPrice = returnListItemModel.UnitPrice;
                                                    mItem.Discount = returnListItemModel.Discount;
                                                    mItem.SalesType = returnListItemModel.SalesType;
                                                    mItem.TaxRate = returnListItemModel.TaxRate;
                                                    mItem.UnitId = returnListItemModel.UnitId;
                                                    mItem.Quantity = returnListItemModel.Quantity;
                                                    mItem.WarehouseId = returnListItemModel.WarehouseId;
                                                    mItem.ProductId = returnListItemModel.ProductId;
                                                    mItem.LocationId = returnListItemModel.LocationId;
                                                    mItem.PackSpec = returnListItemModel.PackSpec;
                                                    mItem.Price0 = returnListItemModel.Price0;
                                                    mItem.Price1 = returnListItemModel.Price1;
                                                    mItem.Price2 = returnListItemModel.Price2;
                                                    mItem.Price3 = returnListItemModel.Price3;
                                                    mItem.Price4 = returnListItemModel.Price4;
                                                    mItem.Price5 = returnListItemModel.Price5;
                                                    mItem.Price6 = returnListItemModel.Price6;
                                                    mItem.Price7 = returnListItemModel.Price7;
                                                    mItem.Price8 = returnListItemModel.Price8;
                                                    mItem.Price9 = returnListItemModel.Price9;
                                                    mItem.Price10 = returnListItemModel.Price10;
                                                    mItem.MinSalesQuantity = returnListItemModel.MinSalesQuantity;
                                                    mItem.MaxSalesQuantity = returnListItemModel.MaxSalesQuantity;
                                                    mItem.MinSalesPrice = returnListItemModel.MinSalesPrice;
                                                    mItem.MaxPurchasePrice = returnListItemModel.MaxPurchasePrice;
                                                    mItem.DefaultLocationName = returnListItemModel.DefaultLocationName;
                                                    mItem.OweRemark = returnListItemModel.Remark;
                                                    mItem.BatchId = "";
                                                    mItem.Code = returnListItemModel.Code;

                                                    LogUtils.e("GoodsItem数量为：" + mItemData.size());
                                                    if (returnListItemModel.ProperyId1 != 0 && returnListItemModel.ProperyId2 != 0) {
                                                        RXSQLite.rx(SQLite.select().from(GoodsPropertyModel.class).where(GoodsPropertyModel_Table.ProductId.eq(mItem.ProductId), GoodsPropertyModel_Table.ParentId.eq(mItem.ProductProperyId1)))
                                                                .queryList()
                                                                .subscribe(new Consumer<List<GoodsPropertyModel>>() {
                                                                    @Override
                                                                    public void accept(@NonNull List<GoodsPropertyModel> goodsPropertyModels) throws Exception {
                                                                        if (goodsPropertyModels != null && goodsPropertyModels.size() > 0) {
                                                                            for (GoodsPropertyModel mModel : goodsPropertyModels) {
                                                                                ProductPropertyListEntity.ValueEntity mProductProperty = new ProductPropertyListEntity.ValueEntity();
                                                                                mProductProperty.Id = mModel.Id;
                                                                                mProductProperty.Name = mModel.Name;
                                                                                mProductProperty.Ordinal = mModel.Ordinal;
                                                                                mProductProperty.ParentId = mModel.ParentId;
                                                                                mProductProperty.Level = mModel.Level;
                                                                                mProductProperty.Path = mModel.Path;
                                                                                mProductProperty.ProductCount = mModel.ProductCount;
                                                                                mItem.ProperyList1.add(mProductProperty);
                                                                            }
                                                                        } else {
                                                                            LogUtils.e("属性1List为空");
                                                                        }
                                                                    }
                                                                });

                                                        RXSQLite.rx(SQLite.select().from(GoodsPropertyModel.class).where(GoodsPropertyModel_Table.ProductId.eq(mItem.ProductId), GoodsPropertyModel_Table.ParentId.eq(mItem.ProductProperyId2)))
                                                                .queryList()
                                                                .subscribe(new Consumer<List<GoodsPropertyModel>>() {
                                                                    @Override
                                                                    public void accept(@NonNull List<GoodsPropertyModel> goodsPropertyModels) throws Exception {
                                                                        if (goodsPropertyModels != null && goodsPropertyModels.size() > 0) {
                                                                            for (GoodsPropertyModel mModel : goodsPropertyModels) {
                                                                                ProductPropertyListEntity.ValueEntity mProductProperty = new ProductPropertyListEntity.ValueEntity();
                                                                                mProductProperty.Id = mModel.Id;
                                                                                mProductProperty.Name = mModel.Name;
                                                                                mProductProperty.Ordinal = mModel.Ordinal;
                                                                                mProductProperty.ParentId = mModel.ParentId;
                                                                                mProductProperty.Level = mModel.Level;
                                                                                mProductProperty.Path = mModel.Path;
                                                                                mProductProperty.ProductCount = mModel.ProductCount;
                                                                                mItem.ProperyList2.add(mProductProperty);
                                                                            }
                                                                        } else {
                                                                            LogUtils.e("属性2List为空");
                                                                        }
                                                                    }
                                                                });

                                                    }
                                                    RXSQLite.rx(SQLite.select().from(ReturnGoodsUnitModel.class).where(ReturnGoodsUnitModel_Table.ProductId.eq(mItem.ProductId)))
                                                            .queryList()
                                                            .subscribe(new Consumer<List<ReturnGoodsUnitModel>>() {
                                                                @Override
                                                                public void accept(@NonNull List<ReturnGoodsUnitModel> goodsUnitModels) throws Exception {
                                                                    if (goodsUnitModels != null && goodsUnitModels.size() > 0) {
                                                                        for (ReturnGoodsUnitModel mModel : goodsUnitModels) {
                                                                            if (mModel.ProductId == mItem.ProductId) {
                                                                                ProductUnitEntity.ValueEntity mUnitData = new ProductUnitEntity.ValueEntity();
                                                                                mUnitData.Id = mModel.Id;
                                                                                mUnitData.ProductId = mModel.ProductId;
                                                                                mUnitData.Name = mModel.Name;
                                                                                mUnitData.Factor = mModel.Factor;
                                                                                mUnitData.BasicFactor = mModel.BasicFactor;
                                                                                mUnitData.IsBasic = mModel.IsBasic;
                                                                                mUnitData.IsDefault = mModel.IsDefault;
                                                                                mUnitData.BreakupNotify = mModel.BreakupNotify;
                                                                                mUnitData.Ordinal = mModel.Ordinal;
                                                                                mItem.ProductUnitList.add(mUnitData);
                                                                            }
                                                                        }
                                                                    } else {
                                                                        LogUtils.e("单位List为空");
                                                                    }
                                                                }
                                                            });
                                                    mItemData.add(mItem);
                                                }
                                            });

                                }
                            }
                        });
            } else {
                RXSQLite.rx(SQLite.select()
                        .from(SaleGoodsItemModel.class))
                        .queryList()
                        .subscribe(new Consumer<List<SaleGoodsItemModel>>() {
                            @Override
                            public void accept(@NonNull List<SaleGoodsItemModel> saleGoodsItemModels) throws Exception {
                                if (saleGoodsItemModels != null && saleGoodsItemModels.size() > 0) {
                                    Flowable.fromIterable(saleGoodsItemModels)
                                            .forEach(new Consumer<SaleGoodsItemModel>() {
                                                @Override
                                                public void accept(@NonNull SaleGoodsItemModel saleGoodsItemModel) throws Exception {
                                                    SaleGoodsItem.ValueEntity mItem = new SaleGoodsItem.ValueEntity();
                                                    mItem.ProductProperyId1 = saleGoodsItemModel.ParentProperyId1;
                                                    mItem.ProductProperyId2 = saleGoodsItemModel.ParentProperyId2;
                                                    mItem.ProperyId1 = saleGoodsItemModel.ProperyId1;
                                                    mItem.ProperyId2 = saleGoodsItemModel.ProperyId2;
                                                    mItem.PriceSystemId = saleGoodsItemModel.PriceSystemId;
                                                    mItem.CurrentPrice = saleGoodsItemModel.CurrentPrice;
                                                    mItem.TotalPrice = saleGoodsItemModel.CurrentPrice;
                                                    mItem.ImagePath = saleGoodsItemModel.Path;
                                                    mItem.ProductName = saleGoodsItemModel.ProductName;
                                                    mItem.ActualPrice = saleGoodsItemModel.UnitPrice;
                                                    mItem.ActualUnitPrice = saleGoodsItemModel.BasicUnitPrice;
                                                    mItem.ProductUnitName = saleGoodsItemModel.ProductUnitName;
                                                    mItem.BasicUnitPrice = saleGoodsItemModel.BasicUnitPrice;
                                                    mItem.UnitPrice = saleGoodsItemModel.UnitPrice;
                                                    mItem.Discount = saleGoodsItemModel.Discount;
                                                    mItem.SalesType = saleGoodsItemModel.SalesType;
                                                    mItem.TaxRate = saleGoodsItemModel.TaxRate;
                                                    mItem.UnitId = saleGoodsItemModel.UnitId;
                                                    mItem.Quantity = saleGoodsItemModel.Quantity;
                                                    mItem.WarehouseId = saleGoodsItemModel.WarehouseId;
                                                    mItem.ProductId = saleGoodsItemModel.ProductId;
                                                    mItem.LocationId = saleGoodsItemModel.LocationId;
                                                    mItem.PackSpec = saleGoodsItemModel.PackSpec;
                                                    mItem.Price0 = saleGoodsItemModel.Price0;
                                                    mItem.Price1 = saleGoodsItemModel.Price1;
                                                    mItem.Price2 = saleGoodsItemModel.Price2;
                                                    mItem.Price3 = saleGoodsItemModel.Price3;
                                                    mItem.Price4 = saleGoodsItemModel.Price4;
                                                    mItem.Price5 = saleGoodsItemModel.Price5;
                                                    mItem.Price6 = saleGoodsItemModel.Price6;
                                                    mItem.Price7 = saleGoodsItemModel.Price7;
                                                    mItem.Price8 = saleGoodsItemModel.Price8;
                                                    mItem.Price9 = saleGoodsItemModel.Price9;
                                                    mItem.Price10 = saleGoodsItemModel.Price10;
                                                    mItem.MinSalesQuantity = saleGoodsItemModel.MinSalesQuantity;
                                                    mItem.MaxSalesQuantity = saleGoodsItemModel.MaxSalesQuantity;
                                                    mItem.MinSalesPrice = saleGoodsItemModel.MinSalesPrice;
                                                    mItem.MaxPurchasePrice = saleGoodsItemModel.MaxPurchasePrice;
                                                    mItem.DefaultLocationName = saleGoodsItemModel.DefaultLocationName;
                                                    mItem.OweRemark = saleGoodsItemModel.Remark;
                                                    mItem.BatchId = "";
                                                    mItem.Code = saleGoodsItemModel.Code;
                                                    LogUtils.e("GoodsItem数量为：" + mItemData.size());
                                                    if (saleGoodsItemModel.ProperyId1 != 0 && saleGoodsItemModel.ProperyId2 != 0) {
                                                        RXSQLite.rx(SQLite.select().from(GoodsPropertyModel.class).where(GoodsPropertyModel_Table.ProductId.eq(mItem.ProductId), GoodsPropertyModel_Table.ParentId.eq(mItem.ProductProperyId1)))
                                                                .queryList()
                                                                .subscribe(new Consumer<List<GoodsPropertyModel>>() {
                                                                    @Override
                                                                    public void accept(@NonNull List<GoodsPropertyModel> goodsPropertyModels) throws Exception {
                                                                        if (goodsPropertyModels != null && goodsPropertyModels.size() > 0) {
                                                                            for (GoodsPropertyModel mModel : goodsPropertyModels) {
                                                                                ProductPropertyListEntity.ValueEntity mProductProperty = new ProductPropertyListEntity.ValueEntity();
                                                                                mProductProperty.Id = mModel.Id;
                                                                                mProductProperty.Name = mModel.Name;
                                                                                mProductProperty.Ordinal = mModel.Ordinal;
                                                                                mProductProperty.ParentId = mModel.ParentId;
                                                                                mProductProperty.Level = mModel.Level;
                                                                                mProductProperty.Path = mModel.Path;
                                                                                mProductProperty.ProductCount = mModel.ProductCount;
                                                                                mItem.ProperyList1.add(mProductProperty);
                                                                            }
                                                                        } else {
                                                                            LogUtils.e("属性1List为空");
                                                                        }
                                                                    }
                                                                });

                                                        RXSQLite.rx(SQLite.select().from(GoodsPropertyModel.class).where(GoodsPropertyModel_Table.ProductId.eq(mItem.ProductId), GoodsPropertyModel_Table.ParentId.eq(mItem.ProductProperyId2)))
                                                                .queryList()
                                                                .subscribe(new Consumer<List<GoodsPropertyModel>>() {
                                                                    @Override
                                                                    public void accept(@NonNull List<GoodsPropertyModel> goodsPropertyModels) throws Exception {
                                                                        if (goodsPropertyModels != null && goodsPropertyModels.size() > 0) {
                                                                            for (GoodsPropertyModel mModel : goodsPropertyModels) {
                                                                                ProductPropertyListEntity.ValueEntity mProductProperty = new ProductPropertyListEntity.ValueEntity();
                                                                                mProductProperty.Id = mModel.Id;
                                                                                mProductProperty.Name = mModel.Name;
                                                                                mProductProperty.Ordinal = mModel.Ordinal;
                                                                                mProductProperty.ParentId = mModel.ParentId;
                                                                                mProductProperty.Level = mModel.Level;
                                                                                mProductProperty.Path = mModel.Path;
                                                                                mProductProperty.ProductCount = mModel.ProductCount;
                                                                                mItem.ProperyList2.add(mProductProperty);
                                                                            }
                                                                        } else {
                                                                            LogUtils.e("属性2List为空");
                                                                        }
                                                                    }
                                                                });

                                                    }
                                                    RXSQLite.rx(SQLite.select().from(GoodsUnitModel.class).where(GoodsUnitModel_Table.ProductId.eq(mItem.ProductId)))
                                                            .queryList()
                                                            .subscribe(new Consumer<List<GoodsUnitModel>>() {
                                                                @Override
                                                                public void accept(@NonNull List<GoodsUnitModel> goodsUnitModels) throws Exception {
                                                                    if (goodsUnitModels != null && goodsUnitModels.size() > 0) {
                                                                        for (GoodsUnitModel mModel : goodsUnitModels) {
                                                                            if (mModel.ProductId == mItem.ProductId) {
                                                                                ProductUnitEntity.ValueEntity mUnitData = new ProductUnitEntity.ValueEntity();
                                                                                mUnitData.Id = mModel.Id;
                                                                                mUnitData.ProductId = mModel.ProductId;
                                                                                mUnitData.Name = mModel.Name;
                                                                                mUnitData.Factor = mModel.Factor;
                                                                                mUnitData.BasicFactor = mModel.BasicFactor;
                                                                                mUnitData.IsBasic = mModel.IsBasic;
                                                                                mUnitData.IsDefault = mModel.IsDefault;
                                                                                mUnitData.BreakupNotify = mModel.BreakupNotify;
                                                                                mUnitData.Ordinal = mModel.Ordinal;
                                                                                mItem.ProductUnitList.add(mUnitData);
                                                                            }
                                                                        }
                                                                    } else {
                                                                        LogUtils.e("单位List为空");
                                                                    }
                                                                }
                                                            });
                                                    mItemData.add(mItem);
                                                }
                                            });

                                }
                            }
                        });

            }
            getCreateOrderInfo();
        } else {
            getOrderData();
        }
    }


    private void getOrderData() {
        if (!CommonUtil.isNetworkAvailable(MettingOrderActivity.this)) {
            ToastUtils.showShortSafe("当前网络不可用,请检查网络设置");
            return;
        }
        RetrofitHelper
                .getFetchOrderApi()
                .fetchOrderInfo("SalesOutBill", "Id=" + orderId, "", AppInfoUtil.getToken())
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CreateOrderEntity>() {


                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull CreateOrderEntity createOrderEntity) {
                        if (!createOrderEntity.HasError && createOrderEntity.Value != null) {
                            orderInfo = createOrderEntity.Value;
                            mItemData.addAll(orderInfo.SalesOutBillItemList);
                            LogUtils.e("GoodsItem数量为：" + mItemData.size());
                            setData(false);
                        } else {
                            showEmptyView();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        showEmptyView();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void init() {
        titleView.setLeftIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ciGoodsListCount.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MettingOrderActivity.this, GoodsListActivity.class);
                intent.putExtra("CreateOrder", isCreate);
                intent.putExtra("orderId", orderId);
                intent.putExtra("saleType", saleType);
                startActivity(intent);
            }
        });

        if (isCreate) {

            ciRemark.getEditText().addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (orderInfo != null) {
                        orderInfo.Remark = s.toString();
                    }
                }
            });

            ciDeliveryDate.setOnItemClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (orderInfo == null) {
                        return;
                    }
                    try {
                        Calendar c = Calendar.getInstance(Locale.CHINA);
                        if (orderInfo != null && orderInfo.DeliveryDate != 0) {
                            c.setTimeInMillis(orderInfo.BillDate * 1000);
                        } else {
                            c.setTimeInMillis(System.currentTimeMillis());
                            orderInfo.DeliveryDate = System.currentTimeMillis() / 1000;
                            orderInfo.CreatedTime = orderInfo.DeliveryDate;
                            orderInfo.ReceiveTime = orderInfo.DeliveryDate;
                            orderInfo.BillDate = orderInfo.DeliveryDate;
                        }
                        DatePickerDialog dialog = new DatePickerDialog(MettingOrderActivity.this, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                Calendar c = Calendar.getInstance(Locale.CHINA);
                                c.set(year, monthOfYear, dayOfMonth);
                                ciDeliveryDate.getEditText().setText(DateUtil.getFormatDate(c.getTimeInMillis()));
                                orderInfo.DeliveryDate = c.getTimeInMillis() / 1000;
                            }
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
                        dialog.show();
                    } catch (Exception e) {
                        LogUtil.e("开单时间", "转换异常");
                    }
                }
            });
            ciAddress.getEditText().addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (orderInfo != null) {
                        orderInfo.DeliveryAddress = s.toString();
                    }
                }
            });

            ciAddress.setTitleNameOnclickListener(new View.OnClickListener() {
                private int mPosition;

                @Override
                public void onClick(View view) {
                    if (mAddressList.size() > 0 && orderInfo != null) {
                        final WheelViewBottomDialog dialog = new WheelViewBottomDialog(MettingOrderActivity.this);
                        List<String> wheelData = new ArrayList<>();
                        for (TraderEntity.ValueEntity.TraderDeliveryAddressListEntity item : mAddressList) {
                            wheelData.add(item.Address);
                            if (item.Id == orderInfo.TraderId) {
                                dialog.setIndex(mAddressList.indexOf(item));
                            }
                        }
                        dialog.setWheelData(wheelData);
                        dialog.setOnWheelItemSelectedListener(new WheelView.OnWheelItemSelectedListener() {
                            @Override
                            public void onItemSelected(int position, Object o) {
                                mPosition = position;
                            }
                        });
                        dialog.setOkBtnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (orderInfo != null) {
                                    orderInfo.DeliveryAddress = mAddressList.get(mPosition).Address;
                                    ciAddress.getEditText().setText(orderInfo.DeliveryAddress);
                                    ciAddress.getEditText().setSelection(orderInfo.DeliveryAddress.length());
                                }
                            }
                        });
                        dialog.setTitle(getString(R.string.wheel_dialog_title_deliveryAddress));
                        dialog.show();
                    } else {
                        Toast.makeText(MettingOrderActivity.this, getString(R.string.toast_content_deliveryAddress), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            ciReceiveMethod.getEditText().addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (orderInfo != null) {
                        orderInfo.ReceiveMethod = s.toString();
                    }
                }
            });

            ciReceiveMethod.setTitleNameOnclickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (receiveMethodList != null) {
                        showReceiveMethodDialog(receiveMethodList);
                    } else {
                        if (!CommonUtil.isNetworkAvailable(MettingOrderActivity.this)) {
                            ToastUtils.showShortSafe("当前网络不可用,请检查网络设置");
                            return;
                        }
                        RetrofitHelper
                                .getReceiveMethodApi()
                                .getReceiveMethodList("SettleMethodList", "", "", "", AppInfoUtil.getToken())
                                .compose(bindToLifecycle())
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<ReceiveMethodListEntity>() {
                                    @Override
                                    public void onSubscribe(@NonNull Disposable d) {

                                    }

                                    @Override
                                    public void onNext(@NonNull ReceiveMethodListEntity receiveMethodListEntity) {
                                        if (!receiveMethodListEntity.HasError && receiveMethodListEntity.Value.size() > 0) {
                                            receiveMethodList = receiveMethodListEntity.Value;
                                            showReceiveMethodDialog(receiveMethodList);
                                        }
                                    }

                                    @Override
                                    public void onError(@NonNull Throwable e) {

                                    }

                                    @Override
                                    public void onComplete() {

                                    }
                                });
                    }
                }
            });
        } else {
            ciRemark.setEditable(false);
            ciAddress.setEditable(false);
            ciReceiveMethod.setEditable(false);
        }
    }

    private void saveOrder() {
        if (!CommonUtil.isNetworkAvailable(MettingOrderActivity.this)) {
            ToastUtils.showShortSafe("当前网络不可用,请检查网络设置");
            return;
        }
        if (orderInfo == null) {
            return;
        }
        orderInfo.SalesOutBillItemList = mItemData;
        orderInfo.OutTypeId = 99;
        LogUtils.e(JsonUtils.serialize(orderInfo));

        if (saleType == 1) {
            LoadingDialog loadingDialog = new LoadingDialog(MettingOrderActivity.this, getString(R.string.submit_order));
            RetrofitHelper
                    .getSaveOrderApi()
                    .saveOrderInfo("SalesOutBill", "", ZipUtil.gzip(JsonUtils.serialize(orderInfo)), AppInfoUtil.getToken())
                    .compose(bindToLifecycle())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<CreateOrderEntity>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            loadingDialog.show();
                        }

                        @Override
                        public void onNext(@NonNull CreateOrderEntity createOrderEntity) {
                            if (!createOrderEntity.HasError && createOrderEntity.Value != null) {
                                orderInfo = createOrderEntity.Value;
                                //                            ToastUtils.showShortSafe("创建订单成功！");
                                LogUtils.e("创建退货单成功！");
                                new AlertDialog.Builder(MettingOrderActivity.this)
                                        .setTitle("提示")
                                        .setMessage("创建退货单成功！")
                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                finish();
                                                //清空退货清单
                                                Delete.table(ReturnListItemModel.class);
                                                Delete.table(ReturnGoodsUnitModel.class);
                                            }
                                        })
                                        .setCancelable(false)
                                        .show();
                            } else {
                                ToastUtils.showShortSafe("创建退货单失败！");
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            ToastUtils.showShortSafe("创建退货单失败,请重新提交！");
                            LogUtils.e("创建退货单失败！onError");
                            loadingDialog.dismiss();
                        }

                        @Override
                        public void onComplete() {
                            loadingDialog.dismiss();
                        }
                    });
        } else {
            LoadingDialog loadingDialog = new LoadingDialog(MettingOrderActivity.this, getString(R.string.submit_order));
            RetrofitHelper
                    .getSaveOrderApi()
                    .saveOrderInfo("SalesOutBill", "", ZipUtil.gzip(JsonUtils.serialize(orderInfo)), AppInfoUtil.getToken())
                    .compose(bindToLifecycle())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<CreateOrderEntity>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            loadingDialog.show();
                        }

                        @Override
                        public void onNext(@NonNull CreateOrderEntity createOrderEntity) {
                            if (!createOrderEntity.HasError && createOrderEntity.Value != null) {
                                orderInfo = createOrderEntity.Value;
                                //                            ToastUtils.showShortSafe("创建订单成功！");
                                LogUtils.e("创建订单成功！");
                                new AlertDialog.Builder(MettingOrderActivity.this)
                                        .setTitle("提示")
                                        .setMessage("创建订单成功！")
                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                finish();
                                                //清空购物车
                                                //     FlowManager.getDatabase(AppDatabase.class).reset(MettingOrderActivity.this);
                                                Delete.table(SaleGoodsItemModel.class);
                                                Delete.table(GoodsUnitModel.class);
                                            }
                                        })
                                        .setCancelable(false)
                                        .show();
                            } else {
                                ToastUtils.showShortSafe("创建订单失败！");
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            loadingDialog.dismiss();
                            ToastUtils.showShortSafe("创建订单失败，请重新提交！");
                            LogUtils.e("创建订单失败！onError");
                        }

                        @Override
                        public void onComplete() {
                            loadingDialog.dismiss();
                        }
                    });
        }

    }

    private void showReceiveMethodDialog(List<ReceiveMethodListEntity.ValueEntity> receiveMethodListEntity) {
        final WheelViewBottomDialog dialog = new WheelViewBottomDialog(MettingOrderActivity.this);
        List<String> wheelData = new ArrayList<>();
        for (ReceiveMethodListEntity.ValueEntity item :
                receiveMethodListEntity) {
            wheelData.add(item.Name);
            if (item.Name.equals(orderInfo.ReceiveMethod)) {
                dialog.setIndex(receiveMethodListEntity.indexOf(item));
            }
        }
        dialog.setWheelData(wheelData);
        dialog.setOnWheelItemSelectedListener(new WheelView.OnWheelItemSelectedListener() {
            @Override
            public void onItemSelected(int position, Object o) {
                mPosition = position;
            }
        });
        dialog.setOkBtnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setReceiveMethod(receiveMethodListEntity.get(mPosition));
            }
        });
        dialog.setTitle("选择收款方式");
        dialog.show();
    }

    /**
     * 设置收款方式
     */
    private void setReceiveMethod(ReceiveMethodListEntity.ValueEntity receiveMethod) {
        if (orderInfo != null) {
            orderInfo.ReceiveMethod = receiveMethod.Name;
        }
        ciReceiveMethod.getEditText().setText(receiveMethod.Name);
    }

    private void getIntentExtra() {
        isCreate = getIntent().getBooleanExtra("CreateOrder", false);
        orderId = getIntent().getIntExtra("orderId", 0);
        saleType = getIntent().getIntExtra("saleType", 0);
        if (saleType == 1) {
            titleView.setTitle("退单详情");
        }
        LogUtils.e("isCreate：" + isCreate + "，orderId：" + orderId + "，saleType：" + saleType);
    }

    private void getCreateOrderInfo() {
        if (!CommonUtil.isNetworkAvailable(MettingOrderActivity.this)) {
            ToastUtils.showShortSafe("当前网络不可用,请检查网络设置");
            return;
        }
        RetrofitHelper
                .getCreateOrderInfo()
                .createOrderInfo("SalesOutBill", "", AppInfoUtil.getToken())
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CreateOrderEntity>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        if (saleType == 1) {
                            LogUtils.e("创建退货单 订阅");
                        } else {
                            LogUtils.e("创建销售单 订阅");
                        }
                    }

                    @Override
                    public void onNext(@NonNull CreateOrderEntity createOrderEntity) {
                        if (!createOrderEntity.HasError) {
                            if (saleType == 1) {
                                LogUtils.e("创建退货单成功");
                            } else {
                                LogUtils.e("创建销售单成功");
                            }
                            orderInfo = createOrderEntity.Value;
                            setData(true);
                        } else {
                            //显示空布局
                            showEmptyView();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        //显示空布局
                        showEmptyView();
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    private void setContact(MettingLoginEntity.ValueEntity contact) {
        if (orderInfo != null) {
            orderInfo.ContactId = contact.Id;
            orderInfo.TraderContactName = contact.ContactName;
            orderInfo.TraderId = contact.TraderId;
            orderInfo.TraderPhone = contact.Mobile;
            ciContact.getEditText().setText(orderInfo.TraderContactName);
            ciPhone.getEditText().setText(orderInfo.TraderPhone);
        }
    }


    private void setData(boolean created) {
        if (orderInfo.SalesOutBillItemList == null) {
            orderInfo.SalesOutBillItemList = new ArrayList<>();
        }
        if (created) {
            MettingLoginEntity.ValueEntity customerEntity = DataSaver.getMettingCustomerInfo();
            if (customerEntity != null) {
                orderInfo.TraderName = customerEntity.ContactName;
                setContact(customerEntity);

                orderInfo.DepartmentId = 100;
                orderInfo.EmployeeId = 1;
                orderInfo.EmployeeName = "管理员";
                orderInfo.DeliveryAddress = customerEntity.Address;
                orderInfo.BillDate = System.currentTimeMillis() / 1000;
                orderInfo.DeliveryDate = orderInfo.BillDate;
                orderInfo.CreatedTime = orderInfo.BillDate;
                orderInfo.ReceiveTime = orderInfo.BillDate;
                orderInfo.InvoiceType = "";
                orderInfo.ReceiveMethod = "";//收款方式

            }
        }

        LogUtils.e("GoodsItem数量为：" + mItemData.size());
        orderCount = mItemData.size();
        for (SaleGoodsItem.ValueEntity valueEntity : mItemData) {
            orderTotal += valueEntity.Quantity;
        }
        ciGoodsListCount.getEditText().setText("共" + orderCount + "款，总数" + orderTotal);

        ciDate.getEditText().setText(DateUtil.getFormatDate(orderInfo.BillDate * 1000));
        ciDeliveryDate.getEditText().setText(DateUtil.getFormatDate(orderInfo.BillDate * 1000));
        ciName.getEditText().setText(orderInfo.TraderName);
        ciAddress.getEditText().setText(orderInfo.DeliveryAddress);
        ciReceiveMethod.getEditText().setText(orderInfo.ReceiveMethod);
        ciRemark.getEditText().setText(orderInfo.Remark);
        ciContact.getEditText().setText(orderInfo.TraderContactName);
        ciPhone.getEditText().setText(orderInfo.TraderPhone);

        if (isCreate) {
            if (saleType == 1) {
                titleView.setTitle("填写退单");
            } else {
                titleView.setTitle("填写订单");
            }
            tvSave.setText("提交");
            RxView.clicks(rlSave)
                    .compose(bindToLifecycle())
                    .throttleFirst(2, TimeUnit.SECONDS)
                    .subscribe(new Consumer<Object>() {

                        @Override
                        public void accept(@NonNull Object o) throws Exception {
                            saveOrder();
                        }
                    });
            Flowable.fromIterable(mItemData)
                    .forEach(new Consumer<SaleGoodsItem.ValueEntity>() {
                        @Override
                        public void accept(@NonNull SaleGoodsItem.ValueEntity valueEntity) throws Exception {
                            orderInfo.Amount += valueEntity.CurrentPrice;
                        }
                    });
        } else {
            switch (orderInfo.Status) {
                case 1://未审核
                    tvSave.setText("待审核");
                    break;
                case 2:
                    tvSave.setText("已审核");
                    break;
                case 4:
                    tvSave.setText("已记账");
                    break;
                case 68:
                    tvSave.setText("已记账");
                    break;
                case 132:
                    tvSave.setText("已结清");
                    break;
            }
        }
        tvTotalPrice.setText(NumberUtil.formatDoubleToString(orderInfo.Amount));
    }


    public void showEmptyView() {
        scrollView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        rlBottomView.setVisibility(View.GONE);
        emptyView.setEmptyImage(R.drawable.ic_data_empty);
        emptyView.setEmptyText("暂无数据");
    }

    public void hideEmptyView() {
        emptyView.setVisibility(View.GONE);
        rlBottomView.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.VISIBLE);
    }

}
