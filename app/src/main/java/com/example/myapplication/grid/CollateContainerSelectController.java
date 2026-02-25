package com.example.myapplication.grid;

import androidx.annotation.NonNull;

import com.example.myapplication.db.dao.KakuninContainerDao;
import com.example.myapplication.db.entity.KakuninContainerEntity;
import com.example.myapplication.time.DateTimeFormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


//============================================================
//　処理概要　:　共通関数
//　関　　数　:　CollateContainerSelectController ..... 照合対象コンテナ選択制御（読込/選択/表示行生成）
//　　　　　　:　loadContainers ..... 未照合コンテナの読込
//　　　　　　:　getContainers ..... 読込済みコンテナ一覧の取得
//　　　　　　:　getDisplayRows ..... 表示用行データの取得
//　　　　　　:　checkSelectedNo ..... 選択番号の妥当性チェック
//　　　　　　:　selectContainer ..... 選択番号に該当するコンテナを選択状態へ設定
//　　　　　　:　getSelectedContainerId ..... 選択中コンテナIDの取得
//　　　　　　:　getSelectedContainerNo ..... 選択中コンテナNoの取得
//　　　　　　:　getSelectedBundleCnt ..... 選択中束数の取得
//　　　　　　:　getSelectedSagyouYmd ..... 選択中作業日の取得
//　　　　　　:　refreshDisplayRows ..... 表示行の再生成
//　　　　　　:　safeStr ..... null安全な文字列化
//============================================================

public class CollateContainerSelectController {

    private final KakuninContainerDao kakuninContainerDao;

    // DAOから取得した未照合コンテナ一覧（保持）
    private final List<KakuninContainerEntity> containers = new ArrayList<>();

    // 一覧表示用（RecyclerViewに渡す）
    private final List<CollateContainerSelectRow> displayRows = new ArrayList<>();

    // 選択中のコンテナ情報（selectContainerで設定）
    private String selectedContainerId;
    private String selectedContainerNo;
    private int selectedBundleCnt;
    private String selectedSagyouYmd;

    //==========================================================
    //　機　能　:　CollateContainerSelectControllerの初期化処理
    //　引　数　:　kakuninContainerDao ..... KakuninContainerDao
    //　戻り値　:　[CollateContainerSelectController] ..... なし
    //==========================================================
    public CollateContainerSelectController(@NonNull KakuninContainerDao kakuninContainerDao) {
        this.kakuninContainerDao = kakuninContainerDao;
    }

    //============================
    //　機　能　:　containersを読み込む
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    public void loadContainers() {

        // 既存の保持リストをクリア
        containers.clear();

        // 未照合コンテナを取得して保持リストへ設定
        containers.addAll(kakuninContainerDao.findUncollated());

        // 表示用行データを再生成
        refreshDisplayRows();
    }

    //====================================================
    //　機　能　:　containersを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<KakuninContainerEntity>] ..... 読込済みコンテナ一覧（読み取り専用）
    //====================================================
    @NonNull
    public List<KakuninContainerEntity> getContainers() {
        // 外部から改変されないよう unmodifiableList を返す
        return Collections.unmodifiableList(containers);
    }

    //=================================================
    //　機　能　:　display Rowsを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<CollateContainerSelectRow>] ..... 表示用行データ（読み取り専用）
    //=================================================
    @NonNull
    public List<CollateContainerSelectRow> getDisplayRows() {
        // 外部から改変されないよう unmodifiableList を返す
        return Collections.unmodifiableList(displayRows);
    }

    //=================================
    //　機　能　:　check Selected Noの処理
    //　引　数　:　selectedNo ..... int
    //　戻り値　:　[String] ..... 結果（OK/エラーメッセージ）
    //=================================
    @NonNull
    public String checkSelectedNo(int selectedNo) {

        // 画面入力は 1始まり想定のため、範囲をチェック
        if (selectedNo <= 0 || selectedNo > containers.size()) {
            return "照合対象№が一覧に存在しません";
        }

        return "OK";
    }

    //=================================
    //　機　能　:　select Containerの処理
    //　引　数　:　selectedNo ..... int
    //　戻り値　:　[String] ..... 結果（OK/エラーメッセージ）
    //=================================
    @NonNull
    public String selectContainer(int selectedNo) {

        // 選択番号の妥当性をチェック
        String check = checkSelectedNo(selectedNo);
        if (!"OK".equals(check)) {
            return check;
        }

        // 1始まり→0始まりへ変換して取得
        KakuninContainerEntity entity = containers.get(selectedNo - 1);

        // 選択中情報をメンバへ保持（nullは空文字、束数はnullなら0）
        selectedContainerId = safeStr(entity.containerId);
        selectedContainerNo = safeStr(entity.containerNo);
        selectedBundleCnt = entity.bundleCnt != null ? entity.bundleCnt : 0;
        selectedSagyouYmd = safeStr(entity.sagyouYmd);

        return "OK";
    }

    //=======================================
    //　機　能　:　selected Container Idを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... 選択中コンテナID
    //=======================================
    public String getSelectedContainerId() {
        return selectedContainerId;
    }

    //=======================================
    //　機　能　:　selected Container Noを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... 選択中コンテナNo
    //=======================================
    public String getSelectedContainerNo() {
        return selectedContainerNo;
    }

    //=====================================
    //　機　能　:　selected Bundle Cntを取得する
    //　引　数　:　なし
    //　戻り値　:　[int] ..... 選択中束数
    //=====================================
    public int getSelectedBundleCnt() {
        return selectedBundleCnt;
    }

    //=====================================
    //　機　能　:　selected Sagyou Ymdを取得する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... 選択中作業日（YYYYMMDD等の生値想定）
    //=====================================
    public String getSelectedSagyouYmd() {
        return selectedSagyouYmd;
    }

    //==============================
    //　機　能　:　display Rowsを更新する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //==============================
    private void refreshDisplayRows() {

        // 表示行を一旦クリアして作り直す
        displayRows.clear();

        // containers の順序で表示行を生成（画面表示は 1始まり）
        for (int i = 0; i < containers.size(); i++) {

            KakuninContainerEntity entity = containers.get(i);

            // 画面表示用に文字列化（束数はnullなら0）
            String no = String.valueOf(i + 1);
            String containerNo = safeStr(entity.containerNo);
            String bundleCnt = String.valueOf(entity.bundleCnt != null ? entity.bundleCnt : 0);

            // 作業日は表示用フォーマットへ変換（ユーティリティに委譲）
            String sagyouYmdDisp = DateTimeFormatUtil.formatSagyouYmdForDisplay(entity.sagyouYmd);

            // 表示行を追加
            displayRows.add(new CollateContainerSelectRow(
                    no,
                    containerNo,
                    bundleCnt,
                    sagyouYmdDisp
            ));
        }
    }

    //===============================
    //　機　能　:　safe Strの処理
    //　引　数　:　value ..... String
    //　戻り値　:　[String] ..... nullなら空文字、非nullはそのまま
    //===============================
    private String safeStr(String value) {
        return value == null ? "" : value;
    }
}