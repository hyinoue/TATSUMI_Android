package com.example.myapplication.grid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.db.dao.KakuninContainerDao;
import com.example.myapplication.db.dao.KakuninMeisaiDao;
import com.example.myapplication.db.dao.KakuninMeisaiWorkDao;
import com.example.myapplication.db.entity.KakuninContainerEntity;
import com.example.myapplication.db.entity.KakuninMeisaiEntity;
import com.example.myapplication.db.entity.KakuninMeisaiWorkEntity;
import com.example.myapplication.time.DateTimeFormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


//============================================================
//　処理概要　:　バンニング照合画面の一覧行生成・更新制御を行うコントローラクラス
//　関　　数　:　VanningCollationController ..... バンニング照合制御（Work作成/照合更新/件数/表示行生成/完了更新）
//　　　　　　:　load ..... コンテナID指定でWork再作成→一覧読込→表示更新
//　　　　　　:　getDetails ..... Work明細の取得
//　　　　　　:　getDisplayRows ..... 表示用行データの取得
//　　　　　　:　checkSokuDtl ..... 束番明細の照合可否チェック（存在/確認済）
//　　　　　　:　updateSyougo ..... 照合済フラグ更新（Work更新→再読込→表示更新）
//　　　　　　:　getSyougouSumiCount ..... 照合済件数の取得
//　　　　　　:　getUncollatedCount ..... 未照合件数の取得
//　　　　　　:　markContainerCollated ..... コンテナ照合完了フラグ更新
//　　　　　　:　refreshDisplayRows ..... 表示行の再生成
//　　　　　　:　setT_KAKUNIN_MEISAItoW_KAKUNIN_MEISAI ..... T→Wへ明細コピー（作業用テーブル作成）
//　　　　　　:　readW_KAKUNIN_MEISAItoList ..... Work明細の読込
//　　　　　　:　toWorkEntity ..... 明細Entity→WorkEntity変換
//　　　　　　:　safeStr ..... null安全な文字列化
//　　　　　　:　isBlank ..... 空/空白判定
//　　　　　　:　repeat ..... 文字列繰り返し生成
//============================================================

public class VanningCollationController {

    private final KakuninMeisaiDao kakuninMeisaiDao;         // 確認明細DAO
    private final KakuninMeisaiWorkDao kakuninMeisaiWorkDao; // 確認明細WorkDAO

    // Work明細（照合状態を保持する作業用リスト）
    private final List<KakuninMeisaiWorkEntity> details = new ArrayList<>();

    // 一覧表示用（RecyclerViewに渡す）
    private final List<VanningCollationRow> displayRows = new ArrayList<>();

    //============================================================
    //　機　能　:　VanningCollationControllerの初期化処理
    //　引　数　:　kakuninMeisaiDao ..... データアクセスオブジェクト
    //　　　　　:　kakuninMeisaiWorkDao ..... データアクセスオブジェクト
    //　戻り値　:　[VanningCollationController] ..... なし
    //============================================================
    public VanningCollationController(@NonNull KakuninMeisaiDao kakuninMeisaiDao,
                                      @NonNull KakuninMeisaiWorkDao kakuninMeisaiWorkDao) {
        this.kakuninMeisaiDao = kakuninMeisaiDao;
        this.kakuninMeisaiWorkDao = kakuninMeisaiWorkDao;
    }

    //============================================================
    //　機　能　:　データを読み込む
    //　引　数　:　containerId ..... ID
    //　戻り値　:　[void] ..... なし
    //============================================================
    public void load(@Nullable String containerId) {

        // 保持中の明細をクリア
        details.clear();

        // コンテナIDが指定されている場合のみWork作成＆読込を行う
        if (containerId != null) {

            // T_KAKUNIN_MEISAI を W_KAKUNIN_MEISAI にコピーして作業テーブルを作成
            setT_KAKUNIN_MEISAItoW_KAKUNIN_MEISAI(containerId);

            // Workテーブルをリストへ読み込み
            readW_KAKUNIN_MEISAItoList();
        }

        // 表示行を再生成
        refreshDisplayRows();
    }

    //============================================================
    //　機　能　:　detailsを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<KakuninMeisaiWorkEntity>] ..... Work明細（読み取り専用）
    //============================================================
    @NonNull
    public List<KakuninMeisaiWorkEntity> getDetails() {
        // 外部から改変されないよう unmodifiableList を返す
        return Collections.unmodifiableList(details);
    }

    //============================================================
    //　機　能　:　表示用行データを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<VanningCollationRow>] ..... 表示用行データ（読み取り専用）
    //============================================================
    @NonNull
    public List<VanningCollationRow> getDisplayRows() {
        // 外部から改変されないよう unmodifiableList を返す
        return Collections.unmodifiableList(displayRows);
    }

    //============================================================
    //　機　能　:　束明細の照合可否を確認する
    //　引　数　:　heatNo ..... ヒートNo
    //　　　　　:　sokuban ..... 束番
    //　戻り値　:　[String] ..... 結果（OK/エラーメッセージ）
    //============================================================
    public String checkSokuDtl(String heatNo, String sokuban) {

        // 入力をトリム（nullは空文字）
        heatNo = heatNo != null ? heatNo.trim() : "";
        sokuban = sokuban != null ? sokuban.trim() : "";

        // 入力不足は対象外扱い
        if (isBlank(heatNo) || isBlank(sokuban)) {
            return "照合対象に存在していません";
        }

        // Workテーブルから該当1件を取得
        KakuninMeisaiWorkEntity entity = kakuninMeisaiWorkDao.findOne(heatNo, sokuban);

        // 存在しない場合
        if (entity == null) {
            return "照合対象に存在していません";
        }

        // 既に照合済みの場合
        if (Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
            return "既に確認済みです";
        }

        return "OK";
    }

    //============================================================
    //　機　能　:　syougoを更新する
    //　引　数　:　heatNo ..... ヒートNo
    //　　　　　:　sokuban ..... 束番
    //　戻り値　:　[void] ..... なし
    //============================================================
    public void updateSyougo(String heatNo, String sokuban) {

        // Workテーブルから該当1件を取得
        KakuninMeisaiWorkEntity entity = kakuninMeisaiWorkDao.findOne(heatNo, sokuban);
        if (entity == null) {
            // 対象が存在しない場合は何もしない
            return;
        }

        // 既に照合済みなら更新不要
        if (Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
            return;
        }

        // 照合済みに更新
        entity.containerSyougoKakunin = true;

        // 更新情報を設定
        entity.updateProcName = "VanningCollationController";
        entity.updateYmd = DateTimeFormatUtil.nowDbYmdHms();

        // Workテーブルへ反映（upsert）
        kakuninMeisaiWorkDao.upsert(entity);

        // 最新状態を再読込して表示更新
        readW_KAKUNIN_MEISAItoList();
        refreshDisplayRows();
    }

    //============================================================
    //　機　能　:　照合済件数を取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 照合済件数
    //============================================================
    public int getSyougouSumiCount() {

        int count = 0;

        // details を走査して照合済みをカウント
        for (KakuninMeisaiWorkEntity entity : details) {
            if (Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
                count++;
            }
        }

        return count;
    }

    //============================================================
    //　機　能　:　未照合件数を取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 未照合件数
    //============================================================
    public int getUncollatedCount() {

        int count = 0;

        // details を走査して未照合をカウント
        for (KakuninMeisaiWorkEntity entity : details) {
            if (!Boolean.TRUE.equals(entity.containerSyougoKakunin)) {
                count++;
            }
        }

        return count;
    }

    //============================================================
    //　機　能　:　コンテナを照合済として更新する
    //　引　数　:　kakuninContainerDao ..... データアクセスオブジェクト
    //　戻り値　:　[void] ..... なし
    //============================================================
    public void markContainerCollated(@NonNull KakuninContainerDao kakuninContainerDao) {

        // 明細が無ければ何もしない
        if (details.isEmpty()) {
            return;
        }

        // 先頭明細から containerId を取得（全明細同一コンテナ想定）
        KakuninMeisaiWorkEntity first = details.get(0);
        if (first.containerId == null) {
            return;
        }

        // コンテナ情報を取得
        KakuninContainerEntity container = kakuninContainerDao.findByContainerId(first.containerId);
        if (container == null) {
            return;
        }

        // 照合完了フラグを立てる
        container.containerSyougoKanryo = true;

        // 送信日時は未送信扱いに戻す（仕様に合わせてnullクリア）
        container.dataSendYmdhms = null;

        // 更新情報を設定
        container.updateProcName = "VanningCollation";
        container.updateYmd = DateTimeFormatUtil.nowDbYmdHms();

        // コンテナを更新（upsert）
        kakuninContainerDao.upsert(container);
    }

    //============================================================
    //　機　能　:　表示用行データを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void refreshDisplayRows() {

        // 一旦クリアして作り直す
        displayRows.clear();

        // Work明細順に表示行を生成
        for (KakuninMeisaiWorkEntity entity : details) {

            // 表示値を安全に取得
            String pNo = safeStr(entity.syukkaSashizuNo);
            String bNo = safeStr(entity.bundleNo);
            String idx = safeStr(entity.sokuban);

            // 重量：3桁区切り + 左6桁幅スペース埋め
            String j = String.format(Locale.JAPAN, "%,d", entity.jyuryo != null ? entity.jyuryo : 0);
            if (j.length() < 6) {
                j = repeat(" ", 6 - j.length()) + j;
            }

            // 照合済み表示（済/空白）
            String confirmed = Boolean.TRUE.equals(entity.containerSyougoKakunin) ? "済" : "　";

            // 表示行を追加
            displayRows.add(new VanningCollationRow(pNo, bNo, idx, j, confirmed));
        }
    }

    //============================================================
    //　機　能　:　t_KAKUNIN_MEISAIto W_KAKUNIN_MEISAIを設定する
    //　引　数　:　containerId ..... ID
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void setT_KAKUNIN_MEISAItoW_KAKUNIN_MEISAI(@NonNull String containerId) {

        // Workテーブルを一旦全削除（コンテナ切替時のゴミを残さない）
        kakuninMeisaiWorkDao.deleteAll();

        // コンテナIDに紐づく明細をTから取得
        List<KakuninMeisaiEntity> source = kakuninMeisaiDao.findByContainerId(containerId);

        // 取得した明細をWorkEntityに変換してWorkへ登録
        for (KakuninMeisaiEntity entity : source) {
            kakuninMeisaiWorkDao.upsert(toWorkEntity(entity));
        }
    }

    //============================================================
    //　機　能　:　W_KAKUNIN_MEISAIを一覧へ読み込む
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================================================
    private void readW_KAKUNIN_MEISAItoList() {

        // 保持中の明細をクリアして、Workの最新を読込
        details.clear();
        details.addAll(kakuninMeisaiWorkDao.findAllOrdered());
    }

    //============================================================
    //　機　能　:　ワークエンティティへ変換する
    //　引　数　:　entity ..... エンティティ情報
    //　戻り値　:　[KakuninMeisaiWorkEntity] ..... WorkEntity
    //============================================================
    private KakuninMeisaiWorkEntity toWorkEntity(KakuninMeisaiEntity entity) {

        // TのEntityをWorkにコピー（照合状態の更新はWork側で行う）
        KakuninMeisaiWorkEntity work = new KakuninMeisaiWorkEntity();

        // キー項目はトリムして格納（nullはnullのまま）
        work.heatNo = entity.heatNo != null ? entity.heatNo.trim() : null;
        work.sokuban = entity.sokuban != null ? entity.sokuban.trim() : null;

        // 表示・判定に必要な項目をコピー
        work.syukkaSashizuNo = entity.syukkaSashizuNo;
        work.bundleNo = entity.bundleNo;
        work.jyuryo = entity.jyuryo;
        work.containerId = entity.containerId;

        // 照合状態をコピー（Tの状態を初期値としてWorkへ）
        work.containerSyougoKakunin = entity.containerSyougoKakunin;

        // 監査/状態系カラムをコピー
        work.kakuninContainerId = entity.kakuninContainerId;
        work.kakuninStatus = entity.kakuninStatus;
        work.insertProcName = entity.insertProcName;
        work.insertYmd = entity.insertYmd;
        work.updateProcName = entity.updateProcName;
        work.updateYmd = entity.updateYmd;
        work.deleteFlg = entity.deleteFlg;

        return work;
    }

    //============================================================
    //　機　能　:　null安全な文字列へ変換する
    //　引　数　:　value ..... 設定値
    //　戻り値　:　[String] ..... nullなら空文字、非nullはそのまま
    //============================================================
    private String safeStr(String value) {
        return value == null ? "" : value;
    }

    //============================================================
    //　機　能　:　空文字かどうか判定する
    //　引　数　:　value ..... 設定値
    //　戻り値　:　[boolean] ..... true:空/空白、false:それ以外
    //============================================================
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    //============================================================
    //　機　能　:　指定文字列を繰り返して作成する
    //　引　数　:　s ..... 文字列
    //　　　　　:　n ..... 数値
    //　戻り値　:　[String] ..... s を n回繰り返した文字列
    //============================================================
    private String repeat(String s, int n) {

        // 繰り返し回数が0以下の場合は空文字
        if (n <= 0) {
            return "";
        }

        // StringBuilderで連結
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }

        return sb.toString();
    }

}
